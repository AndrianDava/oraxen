package io.th0rgal.oraxen.utils.pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.Item;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.settings.Pack;
import io.th0rgal.oraxen.utils.NMS;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.util.*;

public class ResourcePack {

    private static File modelsFolder;

    public static void generate() {

        File packFolder = new File(OraxenPlugin.get().getDataFolder(), "pack");
        if (!packFolder.exists())
            packFolder.mkdirs();

        File texturesFolder = new File(packFolder, "textures");
        modelsFolder = new File(packFolder, "models");
        if (!modelsFolder.exists()) {

        }

        File pack = new File(packFolder, packFolder.getName() + ".zip");

        if (!Boolean.parseBoolean(Pack.GENERATE.toString()))
            return;

        if (pack.exists())
            pack.delete();

        if (!new File(packFolder, "pack.mcmeta").exists())
            OraxenPlugin.get().saveResource("pack" + File.separator + "pack.mcmeta", false);

        if (!new File(packFolder, "pack.png").exists())
            OraxenPlugin.get().saveResource("pack" + File.separator + "pack.png", false);

        // Sorting items to keep only one with models (and generate it if needed)
        Map<Material, List<Item>> texturedItems = new HashMap<>();
        for (Map.Entry<String, Item> entry : OraxenItems.getEntries()) {
            Item item = entry.getValue();
            if (item.hasPackInfos()) {
                if (item.getPackInfos().shouldGenerateModel()) {
                    writeStringToFile(
                            new File(modelsFolder, item.getPackInfos().getModelName() + ".json"),
                            new ModelGenerator(item.getPackInfos()).getJson().toString());
                }
                List<Item> items = texturedItems.getOrDefault(item.getItem().getType(), new ArrayList<>());
                //todo: could be improved by using items.get(i).getPackInfos().getCustomModelData() when items.add(customModelData, item) with catch when not possible
                if (items.isEmpty())
                    items.add(item);
                else
                    for (int i = 0; i < items.size(); i++)
                        if (items.get(i).getPackInfos().getCustomModelData() > item.getPackInfos().getCustomModelData()) {
                            items.add(i, item);
                            break;
                        } else if (i == items.size() - 1) {
                            items.add(item);
                            break;
                        }
                texturedItems.put(item.getItem().getType(), items);
            }
        }
        generatePredicates(texturedItems);

        //zipping resourcepack
        try {
            List<File> rootFolder = new ArrayList<>();
            ZipUtils.getFilesInFolder(packFolder, rootFolder, packFolder.getName() + ".zip");

            List<File> subfolders = new ArrayList<>();
            ZipUtils.getAllFiles(texturesFolder, subfolders);
            ZipUtils.getAllFiles(modelsFolder, subfolders);

            Map<String, List<File>> fileListByZipDirectory = new HashMap<>();
            fileListByZipDirectory.put("assets/minecraft", subfolders);
            fileListByZipDirectory.put("", rootFolder);

            ZipUtils.writeZipFile(packFolder, packFolder, fileListByZipDirectory);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void generatePredicates(Map<Material, List<Item>> texturedItems) {
        File itemsFolder = new File(modelsFolder, "item");
        if (!itemsFolder.exists())
            itemsFolder.mkdirs();
        for (Map.Entry<Material, List<Item>> texturedItemsEntry : texturedItems.entrySet()) {
            PredicatesGenerator predicatesGenerator = new PredicatesGenerator(texturedItemsEntry.getKey(), texturedItemsEntry.getValue());
            writeStringToFile(
                    new File(modelsFolder, predicatesGenerator.getVanillaModelName(texturedItemsEntry.getKey()) + ".json"),
                    predicatesGenerator.toJSON().toString());
        }
    }

    private static void writeStringToFile(File file, String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void send(Player player) {
        Class<?> PacketClass = NMS.PACKET_PLAY_OUT_RESOURCE_PACK_SEND.toClass();
        try {
            Constructor<?> packetConstructor = PacketClass.getConstructor(String.class, String.class);
            Object packet = packetConstructor.newInstance(Pack.URL.toString(), Pack.SHA1.toString());
            NMS.sendPacket(player, packet);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}