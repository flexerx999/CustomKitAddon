# 🎉 CustomKitAddon - Customkit Silly Addon

**Multi-page custom items GUI** - customisable amount of pages with navigation, opened on clicking on blank slots in **/customkit**.
**Admin commands** - [ /customkitadmin add ] and [ /customkitadmin reload ].
**Permissions** - customkit.use and customkit.admin
**Full YAML config** - Custom items with all types of data involved, metadata and skulls support.
**Enchantment GUI** - Added a Custom GUI for enchants, different type of enchants list for each different item.
**Pre-made Items** - All types of supported items with options involved for enchants gui installed on jar drop.

# CustomKitAddon Project Structure + Instructions
```
CustomKitAddon/
│
├── libs/
│   └── strikepractice-api.jar (download from API site) + make a folder called libs in the root then drop the api jar in it.
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── sp/
│       │           └── customkitaddon/
│       │               ├── CustomKitAddon.java (Main plugin class)
│       │               │
│       │               ├── commands/
│       │               │   ├── AdminCommand.java
│       │               │   └── CustomKitCommand.java
│       │               │   └── DebugCommand.java
│       │               │
│       │               ├── config/
│       │               │   ├── ConfigManager.java
│       │               │   └── ItemsConfig.java
│       │               │
│       │               ├── gui/
│       │               │   ├── CustomItemsGUI.java
│       │               │   ├── EnchantmentGUI.java
│       │               │   └── GUIManager.java
│       │               │
│       │               ├── listeners/
│       │               │   ├── EloChangeListener.java
│       │               │   ├── InventoryClickListener.java
│       │               │   └── SpListener.java
│       │               │
│       │               ├── models/
│       │               │   └── CustomItem.java
│       │               │
│       │               └── utils/
│       │                   ├── ItemBuilder.java
│       │                   └── MessageUtil.java
│       │
│       └── resources/
│           ├── plugin.yml
│           ├── config.yml
│           └── items.yml
│
├── target/ (generated after build)
│   └── CustomKitAddon-1.0.jar
│
├── pom.xml (Maven configuration)
├── README.md (Documentation)
└── .gitignore (Git ignore file)```
