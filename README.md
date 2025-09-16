# 🎉 CustomKitAddon - Strikepractice Customkit Silly Addon

**Multi-page custom items GUI** - 3 paginated pages with navigation, opened on clicking on blank slots in **/customkit**
**Mending enchantment** - Added to armor enchantment section alongside Protection and Unbreaking
**Chat-based renaming kits** - With support of color code and timeout feature
**Admin commands** - /customkitadmin add and /customkitadmin reload
**Full YAML config** - Custom items with enchantments, attributes and special properties


# CustomKitAddon Project Structure
```
CustomKitAddon/
│
├── libs/
│   └── strikepractice-api.jar (download from API site)
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── strikepractice/
│       │           └── customkitaddon/
│       │               ├── CustomKitAddon.java (Main plugin class)
│       │               │
│       │               ├── commands/
│       │               │   ├── AdminCommand.java
│       │               │   └── CustomKitCommand.java
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
│       │               │   ├── ChatListener.java
│       │               │   ├── InventoryClickListener.java
│       │               │   └── StrikePracticeListener.java
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
│   └── CustomKitAddon-1.0.0.jar
│
├── pom.xml (Maven configuration)
├── README.md (Documentation)
└── .gitignore (Git ignore file)```
