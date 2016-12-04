![android frontia](doc/banner_frontia.jpg "android frontia")

#### Frontia
Android-Frontia是一个Android的插件化框架（基于ClassLoader的动态加载技术），相比其他开源项目，Frontia 的特点是扩展性强，更加专注于插件的下载、更新、安装、管理，以及插件和宿主之间的交互。

#### 特点
- 实现了插件下载、更新逻辑（插件版本控制）；
- 实现了插件的安装（插件管理）；
- 实现了插件的加载逻辑（插件使用）；
- 实现插件的签名校验（插件安全）；
- 插件的更新、安装以及加载分别由各自的类负责，可以通过重写这些类来实现自定义的逻辑（具有高扩展性）；
- 多种插件化方式，比如“加载SO库”、“加载Fragment”、“加载Activity/Service组件”（满足不同业务情景）；
- 使用插件行为接口来控制加载的插件，不直接使用反射；
- 提供插件调用宿主功能的HostApi接口，插件可以通过这些接口访问宿主的API；

#### TODO
- [x] 支持Assets内置插件；
- [x] 支持res资源的插件；
- [x] 支持SO库的插件；
- [ ] 支持新建基础组件的插件（Activity等）；
- [x] 插件下载和更新（PluginUpdater）；
- [x] 插件安装（PluginInstaller）；
- [x] 插件加载（PluginLoader）；
- [ ] 增加Config类，支持自定义；
- [ ] 添加AndroidTestCase；
- [ ] 完善DEMO项目；
- [ ] 添加Usage演示代码；
- [x] 插件与宿主间的通讯（HostApiManager）；
- [ ] 优化插件安装策略（减少文件操作，提高稳定性）；
- [ ] 优化下载逻辑（多线程下载，断点续传）；
