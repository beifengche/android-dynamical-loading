package moe.studio.frontia;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;

/**
 * 包名内部工具类。
 * 開けよ、我が闇の力！
 * <p>
 * {@linkplain FileUtils}       文件操作
 * {@linkplain SoLibUtils}      SO库操作
 * {@linkplain ApkUtils}        APK操作
 * {@linkplain SignatureUtils}  签名验证
 *
 * @author kaede
 * @version date 16/10/20
 */

class Internals {

    static class FileUtils {
        public static final String TAG = "plugin.files";

        static void closeQuietly(Closeable closeable) {
            IOUtils.closeQuietly(closeable);
        }

        static void delete(File file) {
            deleteQuietly(file);
        }

        static boolean deleteQuietly(File file) {
            return org.apache.commons.io.FileUtils.deleteQuietly(file);
        }

        static void checkCreateFile(File file) throws IOException {
            if (file == null) {
                throw new IOException("File is null.");
            }
            if (file.exists()) {
                deleteQuietly(file);
            }
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs(); // Ignore.
            }
            if (!file.createNewFile()) {
                throw new IOException("Create file fail, file already exists.");
            }
        }

        static void checkCreateDir(File file) throws IOException {
            if (file == null) {
                throw new IOException("Dir is null.");
            }
            if (file.exists()) {
                if (file.isDirectory()) {
                    return;
                }
                if (!deleteQuietly(file)) {
                    throw new IOException("Fail to delete existing file, file = "
                            + file.getAbsolutePath());
                }
                file.mkdir(); // Ignore.
            } else {
                file.mkdirs(); // Ignore.
            }
            if (!file.exists() || !file.isDirectory()) {
                throw new IOException("Fail to create dir, dir = " + file.getAbsolutePath());
            }
        }

        static void copyFile(File sourceFile, File destFile) throws IOException {
            if (sourceFile == null) {
                throw new IOException("Source file is null.");
            }
            if (destFile == null) {
                throw new IOException("Dest file is null.");
            }
            if (!sourceFile.exists()) {
                throw new IOException("Source file not found.");
            }

            checkCreateFile(destFile);
            InputStream in = null;
            OutputStream out = null;

            try {
                in = new FileInputStream(sourceFile);
                out = new FileOutputStream(destFile);
                FileDescriptor fd = ((FileOutputStream) out).getFD();
                out = new BufferedOutputStream(out);
                IOUtils.copy(in, out);
                out.flush();
                fd.sync();
            } catch (IOException e) {
                if (Logger.DEBUG) {
                    e.printStackTrace();
                }
            } finally {
                closeQuietly(in);
                closeQuietly(out);
            }
        }

        static void copyFileFromAsset(Context context, String pathAssets, File destFile)
                throws IOException {
            if (TextUtils.isEmpty(pathAssets)) {
                throw new IOException("Asset path is empty.");
            }

            checkCreateFile(destFile);
            InputStream in = null;
            OutputStream out = null;

            try {
                in = context.getAssets().open(pathAssets);
                out = new FileOutputStream(destFile);
                FileDescriptor fd = ((FileOutputStream) out).getFD();
                out = new BufferedOutputStream(out);
                IOUtils.copy(in, out);
                out.flush();
                fd.sync();
            } catch (IOException e) {
                if (Logger.DEBUG) {
                    e.printStackTrace();
                }
            } finally {
                closeQuietly(in);
                closeQuietly(out);
            }
        }

        static void dumpFiles(File file) {
            if (!Logger.DEBUG) {
                return;
            }

            boolean isDirectory = file.isDirectory();
            Logger.v(TAG, "path = " + file.getAbsolutePath() + ", isDir = " + isDirectory);
            if (isDirectory) {
                File[] childFiles = file.listFiles();
                if (childFiles != null && childFiles.length > 0) {
                    for (File childFile : childFiles) {
                        dumpFiles(childFile);
                    }
                }
            }
        }
    }

    static class SoLibUtils {
        static final String TAG = "plugin.so";
        private static final int BUFFER_SIZE = 1024 * 4;

        /**
         * 将SO库解压到指定路径并返回所有解压好的SO库文件名字的集合。
         *
         * @return SO库文件名集合
         */
        static Set<String> extractSoLib(File apkFile, File destDir) throws IOException {
            if (apkFile == null || !apkFile.exists()) {
                throw new IOException("Apk file not found.");
            }

            HashSet<String> result = new HashSet<String>(4);
            FileUtils.checkCreateDir(destDir);
            Logger.v(TAG, "copy so file to " + destDir.getAbsolutePath()
                    + ", apk = " + apkFile.getName());

            ZipFile zipFile = null;
            InputStream in = null;
            OutputStream out = null;

            try {
                zipFile = new ZipFile(apkFile);
                ZipEntry zipEntry;
                Enumeration entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    zipEntry = (ZipEntry) entries.nextElement();
                    String relativePath = zipEntry.getName();

                    if (relativePath == null || relativePath.contains("../")) {
                        // Abort zip file injection hack.
                        continue;
                    }

                    if (!relativePath.startsWith("lib" + File.separator)) {
                        Logger.v(TAG, "not lib dir entry, skip " + relativePath);
                        continue;
                    }

                    if (zipEntry.isDirectory()) {
                        File folder = new File(destDir, relativePath);
                        Logger.v(TAG, "create dir " + folder.getAbsolutePath());
                        FileUtils.checkCreateDir(folder);
                    } else {
                        File soLibFile = new File(destDir, relativePath);
                        Logger.v(TAG, "unzip soLib file " + soLibFile.getAbsolutePath());
                        FileUtils.checkCreateFile(soLibFile);

                        byte[] buffer = new byte[BUFFER_SIZE];
                        out = new FileOutputStream(soLibFile);
                        FileDescriptor fd = ((FileOutputStream) out).getFD();
                        out = new BufferedOutputStream(out);
                        in = new BufferedInputStream(zipFile.getInputStream(zipEntry));
                        int count;
                        while ((count = in.read(buffer)) != -1) {
                            out.write(buffer, 0, count);
                        }
                        out.flush();
                        fd.sync();

                        result.add(soLibFile.getName());
                    }
                }
            } catch (IOException e) {
                if (Logger.DEBUG) {
                    e.printStackTrace();
                }
                throw new IOException("Unzip soLibs fail.", e);
            } finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(out);
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (IOException e) {
                        if (Logger.DEBUG) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            if (Logger.DEBUG) {
                Logger.v(TAG, "--");
                for (String item : result) {
                    Logger.v(TAG, item);
                }
                Logger.v(TAG, "--");
            }
            return result;
        }

        /**
         * 根据so库的名字以及当前系统的CPU类型，复制最合适的so库到目标路径。
         *
         * @param sourceDir so库所在目录
         * @param soLibName so库名字
         * @param destDir   目标so库目录
         * @return 是否成功
         */
        static boolean copySoLib(File sourceDir, String soLibName, File destDir)
                throws IOException {
            boolean hasMatch = false;
            Logger.v(TAG, "--");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // TODO: 2016/11/25 Api21 x64 device may not be supported here.
                String[] abis = Build.SUPPORTED_ABIS;
                if (abis != null) {
                    for (String abi : abis) {
                        Logger.v(TAG, "Try install soLib, supported abi = " + abi);
                        String name = "lib" + File.separator + abi + File.separator + soLibName;
                        File sourceFile = new File(sourceDir, name);
                        if (sourceFile.exists()) {
                            hasMatch = true;
                            File destFile = new File(destDir, soLibName);
                            if (sourceFile.renameTo(destFile)) {
                                Logger.v(TAG, "Rename soLib, from = " + sourceFile.getAbsolutePath()
                                        + ", to = " + destFile.getAbsolutePath());
                            } else {
                                throw new IOException("Rename soLib fail.");
                            }
                            break;
                        }
                    }
                } else {
                    Logger.w(TAG, "Cpu abis is null.");
                }

            } else {
                Logger.v(TAG, "Try install soLib, abi1 = " + Build.CPU_ABI + ", abi2 = " + Build.CPU_ABI2);
                String name = "lib" + File.separator + Build.CPU_ABI + File.separator + soLibName;
                File sourceFile = new File(sourceDir, name);
                if (!sourceFile.exists() && Build.CPU_ABI2 != null) {
                    name = "lib" + File.separator + Build.CPU_ABI2 + File.separator + soLibName;
                    sourceFile = new File(sourceDir, name);
                    if (!sourceFile.exists()) {
                        name = "lib" + File.separator + "armeabi" + File.separator + soLibName;
                        sourceFile = new File(sourceDir, name);
                    }
                }
                if (sourceFile.exists()) {
                    hasMatch = true;
                    File destFile = new File(destDir, soLibName);
                    if (sourceFile.renameTo(destFile)) {
                        Logger.v(TAG, "Rename soLib, from = " + sourceFile.getAbsolutePath()
                                + ", to = " + destFile.getAbsolutePath());
                    } else {
                        throw new IOException("Rename soLib fail.");
                    }
                }
            }

            if (!hasMatch) {
                Logger.d(TAG, "Can not install " + soLibName + ", NO_MATCHING_ABIS");
            }
            Logger.v(TAG, "--");
            return hasMatch;
        }
    }

    // TODO: 2016/11/25 add PluginError.
    static class ApkUtils {

        /**
         * 创建一个ClassLoader实例，用于加载插件里的类。
         *
         * @param context       宿主Context实例
         * @param dexPath       插件路径
         * @param optimizedDir  用于释放插件odex的路径
         * @param nativeLibDir  插件so库路径
         * @param isInDependent 插件类是否与宿主隔离
         * @return 插件ClassLoader实例
         */
        static DexClassLoader createClassLoader(Context context, String dexPath,
                                                String optimizedDir, String nativeLibDir,
                                                boolean isInDependent) {
            ClassLoader parentClassLoader;

            if (isInDependent) {
                // Separate the new ClassLoader from current app, thus the class loaded by this
                // new ClassLoader will deadly incompatible from the current app.
                parentClassLoader = ClassLoader.getSystemClassLoader().getParent();
            } else {
                // Use the current app's ClassLoader as the new ClassLoader's parent.
                // In this case, the class loaded by the new ClassLoader must regard the
                // "Parent Delegation Model" of ClassLoader.
                parentClassLoader = context.getClassLoader();
            }

            try {
                return new DexClassLoader(dexPath, optimizedDir, nativeLibDir, parentClassLoader);
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        /**
         * 创建一个AssetManager实例，用于加载插件的res资源。
         *
         * @param dexPath 插件路径
         * @return 插件AssetManager实例
         */
        static AssetManager createAssetManager(String dexPath) {
            try {
                // TODO: 2016/11/25 We may need to support different api levels here.
                AssetManager assetManager = AssetManager.class.newInstance();
                Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
                addAssetPath.invoke(assetManager, dexPath);
                return assetManager;

            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        static Resources createResources(Context context, AssetManager assetManager) {
            Resources superRes = context.getResources();
            return new Resources(assetManager, superRes.getDisplayMetrics(),
                    superRes.getConfiguration());
        }

        static Class<?> loadClass(ClassLoader classLoader, String className) {
            return loadClass(classLoader, className, false);
        }

        static Class<?> loadClass(ClassLoader classLoader, String className,
                                  boolean shouldInitialize) {
            try {
                return Class.forName(className, shouldInitialize, classLoader);
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        @Nullable
        static PackageInfo getLocalPackageInfo(Context context) {
            return getLocalPackageInfo(context, 0);
        }

        @Nullable
        static PackageInfo getLocalPackageInfo(Context context, int flag) {
            PackageManager pm = context.getPackageManager();
            PackageInfo pkgInfo = null;
            try {
                pkgInfo = pm.getPackageInfo(context.getPackageName(), flag);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return pkgInfo;
        }

        @Nullable
        static PackageInfo getPackageInfo(Context context, String apkPath) {
            return getPackageInfo(context, apkPath,
                    PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES);
        }

        @Nullable
        static PackageInfo getPackageInfo(Context context, String apkPath, int flag) {
            return context.getPackageManager().getPackageArchiveInfo(apkPath, flag);
        }
    }

    static class SignatureUtils {

        private static String TAG = "plugin.signature";
        private static final int BUFFER_SIZE = 1024 * 4;

        /**
         * 获取当前APP的签名
         */
        @Nullable
        @SuppressLint("PackageManagerGetSignatures")
        public static Signature[] getSignatures(Context context) {
            Signature[] signatures = null;
            try {
                PackageInfo pkgInfo = context.getPackageManager()
                        .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
                signatures = pkgInfo.signatures;

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                Logger.w(TAG, "Can not get signature, error = " + e.getLocalizedMessage());
            }
            return signatures;
        }

        /**
         * 获取指定文件的签名
         */
        @Nullable
        static Signature[] getSignatures(Context context, String apkPath) {
            Signature[] signatures = getArchiveSignatures(context, apkPath);
            if (signatures == null) {
                signatures = getArchiveSignatures(apkPath, false);
                if (signatures == null) {
                    signatures = getArchiveSignatures(apkPath, true);
                }
            }
            return signatures;
        }


        @SuppressLint("PackageManagerGetSignatures")
        private static Signature[] getArchiveSignatures(Context context, String apkPath) {
            PackageInfo pkgInfo
                    = context.getPackageManager().getPackageArchiveInfo(apkPath,
                    PackageManager.GET_SIGNATURES);
            return pkgInfo == null ? null : pkgInfo.signatures;
        }

        /**
         * 获取指定文件的签名
         */
        @Nullable
        static Signature[] getArchiveSignatures(String apkPath, boolean simpleMode) {
            Signature signatures[];
            JarFile jarFile = null;

            try {
                byte[] readBuffer = new byte[BUFFER_SIZE];
                jarFile = new JarFile(apkPath);
                Certificate[] certs = null;
                if (simpleMode) {
                    // if SIMPLE MODE,, then we
                    // can trust it...  we'll just use the AndroidManifest.xml
                    // to retrieve its signatures, not validating all of the
                    // files.
                    JarEntry jarEntry = jarFile.getJarEntry("AndroidManifest.xml");
                    certs = loadCertificates(jarFile, jarEntry, readBuffer);
                    if (certs == null) {
                        Logger.w(TAG, "Package "
                                + " has no certificates at entry "
                                + jarEntry.getName() + "; ignoring!");
                        Logger.w(TAG, "INSTALL_PARSE_FAILED_NO_CERTIFICATES");
                        return null;
                    }
                    if (BuildConfig.DEBUG) {
                        Logger.v(TAG, "File " + apkPath + ": entry=" + jarEntry
                                + " certs=" + (certs != null ? certs.length : 0));
                        if (certs != null) {
                            for (Certificate cert : certs) {
                                Logger.d(TAG, "  Public key: "
                                        + Arrays.toString(cert.getPublicKey().getEncoded())
                                        + " " + cert.getPublicKey());
                            }
                        }
                    }
                } else {
                    Enumeration entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry je = (JarEntry) entries.nextElement();
                        if (je.isDirectory()) continue;
                        if (je.getName().startsWith("META-INF/")) continue;
                        Certificate[] localCerts = loadCertificates(jarFile, je,
                                readBuffer);
                        if (BuildConfig.DEBUG) {
                            Logger.v(TAG, "File " + apkPath + " entry " + je.getName()
                                    + ": certs=" + Arrays.toString(certs) + " ("
                                    + (certs != null ? certs.length : 0) + ")");
                        }
                        if (localCerts == null) {
                            Logger.w(TAG, "Package "
                                    + " has no certificates at entry "
                                    + je.getName() + "; ignoring!");
                            Logger.w(TAG, "INSTALL_PARSE_FAILED_NO_CERTIFICATES");
                            return null;
                        } else if (certs == null) {
                            certs = localCerts;
                        } else {
                            // Ensure all certificates match.
                            for (Certificate cert : certs) {
                                boolean found = false;
                                for (Certificate localCert : localCerts) {
                                    if (cert != null &&
                                            cert.equals(localCert)) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found || certs.length != localCerts.length) {
                                    Logger.w(TAG, "Package "
                                            + " has mismatched certificates at entry "
                                            + je.getName() + "; ignoring!");
                                    Logger.w(TAG, "INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES");
                                    return null;
                                }
                            }
                        }
                    }
                }
                if (certs != null && certs.length > 0) {
                    final int N = certs.length;
                    signatures = new Signature[certs.length];
                    for (int i = 0; i < N; i++) {
                        signatures[i] = new Signature(
                                certs[i].getEncoded());
                    }
                } else {
                    Logger.w(TAG, "Package "
                            + " has no certificates; ignoring!");
                    Logger.w(TAG, "INSTALL_PARSE_FAILED_NO_CERTIFICATES");
                    return null;
                }
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
                Logger.w(TAG, "Exception reading " + apkPath);
                Logger.w(TAG, "INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING");
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                Logger.w(TAG, "Exception reading " + apkPath);
                Logger.w(TAG, "INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING");
                return null;
            } catch (RuntimeException e) {
                e.printStackTrace();
                Logger.w(TAG, "Exception reading " + apkPath);
                Logger.w(TAG, "INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION");
                return null;
            } finally {
                if (jarFile != null) {
                    FileUtils.closeQuietly(jarFile);
                }
            }
            return signatures;
        }


        private static Certificate[] loadCertificates(JarFile jarFile, JarEntry je, byte[] readBuffer) {
            InputStream in = null;
            try {
                // We must read the stream for the JarEntry to retrieve
                // its certificates.
                in = new BufferedInputStream(jarFile.getInputStream(je));
                while (in.read(readBuffer, 0, readBuffer.length) != -1) {
                    // Do nothing.
                }
                return je != null ? je.getCertificates() : null;

            } catch (IOException | RuntimeException e) {
                e.printStackTrace();
                Logger.w(TAG, "Exception reading " + je.getName() + " in "
                        + jarFile.getName());
            } finally {
                FileUtils.closeQuietly(in);
            }
            return null;
        }

        public static boolean isSignaturesSame(Signature[] s1, Signature[] s2) {
            if (s1 == null || s2 == null) {
                return false;
            }
            if (s1.length != s2.length) {
                return false;
            }

            HashSet<Signature> set1 = new HashSet<>();
            Collections.addAll(set1, s1);
            HashSet<Signature> set2 = new HashSet<>();
            Collections.addAll(set2, s2);

            // Make sure s2 contains all signatures in s1.
            return set1.equals(set2);
        }

        static boolean isSignaturesSame(String s1, Signature[] s2) {
            if (TextUtils.isEmpty(s1)) {
                return false;
            }
            if (s2 == null) {
                return false;
            }

            for (Signature signature : s2) {
                String item = signature.toCharsString().toLowerCase();
                if (item.equalsIgnoreCase(s1)) {
                    return true;
                }
            }
            return false;
        }

        static void printSignature(Signature[] s) {
            Logger.v(TAG, "-");
            if (s == null || s.length == 0) {
                Logger.v(TAG, "Signature is empty.");
            } else {
                int length = s.length;
                for (int i = 0; i < length; i++) {
                    Logger.v(TAG, "Signature " + i + " = " + s[i].toCharsString().toLowerCase());
                }
            }
            Logger.v(TAG, "-");
        }
    }
}
