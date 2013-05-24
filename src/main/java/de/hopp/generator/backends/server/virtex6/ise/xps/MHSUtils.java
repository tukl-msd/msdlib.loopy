package de.hopp.generator.backends.server.virtex6.ise.xps;

import de.hopp.generator.parser.Attribute;
import de.hopp.generator.parser.Attributes;
import de.hopp.generator.parser.Block;
import de.hopp.generator.parser.Blocks;
import de.hopp.generator.parser.MHSFile;

public class MHSUtils {
    public static MHSFile add(MHSFile file, Attributes attr) {
        return file.replaceAttributes(file.attributes().addAll(attr));
    }
    public static MHSFile add(MHSFile file, Blocks blocks) {
        return file.replaceBlocks(file.blocks().addAll(blocks));
    }
    public static MHSFile add(MHSFile file, Attribute attr) {
        return file.replaceAttributes(file.attributes().add(attr));
    }
    public static MHSFile add(MHSFile file, Block block) {
        return file.replaceBlocks(file.blocks().add(block));
    }
    public static Block add(Block block, Attributes attr) {
        return block.replaceAttributes(block.attributes().addAll(attr));
    }
    public static Block add(Block block, Attribute attr) {
        return block.replaceAttributes(block.attributes().add(attr));
    }
}
