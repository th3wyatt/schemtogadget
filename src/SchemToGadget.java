import org.jnbt.*;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SchemToGadget {

    private static Tag getChildTag(Map<String, Tag> items, String key, Class<? extends Tag> expected) {
        Tag tag = items.get(key);
        return tag;
    }

    SchemToGadget() {
        try {

            //open file and stream in NBT data
            FileInputStream inputStream = new FileInputStream("test2.schematic");
            NBTInputStream nbtInputStream = new NBTInputStream(inputStream);
            CompoundTag backupTag = (CompoundTag) nbtInputStream.readTag();

            System.out.print(backupTag);

            //stores the outer NBT combo tag in a map
            Map<String, Tag> tagCollection = backupTag.getValue();

            //picking out elements in the outer tag
            short width = (Short)getChildTag(tagCollection, "Width", ShortTag.class).getValue();
            short height = (Short) getChildTag(tagCollection, "Height", ShortTag.class).getValue();
            short length = (Short) getChildTag(tagCollection, "Length", ShortTag.class).getValue();

            //storing block ids into an array
            byte[] blocksPrim = (byte[]) getChildTag(tagCollection, "Blocks", ByteArrayTag.class).getValue();

            //converting into a new array as object //TODO better way to do this?
            Byte blocks[] = new Byte[blocksPrim.length];

           //copying elements
            for(int i = 0; i < blocksPrim.length; i ++){
                blocks[i] = blocksPrim[i];
            }

            List<Byte> blockList = Arrays.asList(blocks); // converting to list TODO can prob skip previous step
            List<Byte> blocksSeen = new ArrayList<>(); // to make the elements in stateIntList unique
            List<Integer> stateIntList = new ArrayList<>(); // List to stage for stateIntArray field
            HashMap<Integer, Byte> blockMap = new HashMap<>(); //maps the stateIntArray item to a block id

            //stores the mapped ints for use in stateIntArray field


            int count = 0;
            for (byte block: blockList){
                if (block != 0 && !blocksSeen.contains(block)){
                    blocksSeen.add(block);
                    stateIntList.add(++count);
                    blockMap.put(count, block);
                }else{
                    for (int i = 0; i < stateIntList.size(); i++){
                        if (block == blockMap.get(stateIntList.get(i))){
                            stateIntList.add(stateIntList.get(i));
                            break;
                        }
                    }
                }

            }

            int[] stateIntArray = stateIntList.stream().mapToInt(i -> i).toArray(); //streams stateIntList to array

           //maps block ids to block names.  TODO overload getChildTag with generic method
            Map<String, Tag> blockIDCollection = (Map) getChildTag(tagCollection,"BlockIDs", CompoundTag.class).getValue();

            //Debug prints out the names of the blocks used in the schematic
            for (byte block : blocks){
                String blockString = (String) getChildTag(blockIDCollection, String.valueOf(block), StringTag.class).getValue();
                System.out.print(blockString + " , ");
            }

            File outputFile = new File("output.txt"); //output file name.  placeholder until GUI is wrapped
            FileWriter outWriter = new FileWriter(outputFile);

            //begins building the BuildingGadgets input string
            outWriter.append("{stateIntArray:[I;");

            //adds in the stateIntArray field
            for(int i = 0; i <stateIntArray.length; i ++){
                if (i == (stateIntArray.length -1))
                    outWriter.append(String.valueOf(stateIntArray[i]));
                else {
                    outWriter.append(String.valueOf(stateIntArray[i]));
                    outWriter.append(",");
                }
            }

            //builds the posIntArray
            outWriter.append("],dim:0,posIntArray:[I;");

            int startPos [] = {0,0,0};
            int endPos [] = {length, height, width};

            ArrayList<Integer> posIntList = new ArrayList<>();

            int positionCount = 0;

            for (int y = 0; y < height; y ++){
                for (int z = 0; z < width; z ++){
                    for (int x = 0; x < length; x ++){
                        if(!blockList.get(positionCount++).equals((byte)0)) //ignores air blocks
                            posIntList.add(relPosToInt(x,y,z));
                    }
                }
            }

            int[] posIntArray = posIntList.stream().mapToInt(i -> i).toArray();

            //populates the posIntArray field
            for (int i = 0; i < posIntArray.length; i ++){
                if (i == (posIntArray.length -1))
                    outWriter.append(String.valueOf(posIntArray[i]));
                else {
                    outWriter.append(String.valueOf(posIntArray[i]));
                    outWriter.append(",");
                }
            }

            outWriter.append("],startPos:{X:0,Y:0,Z:0},mapIntState:[{mapSlot:");

            for (int i =0; i < stateIntArray.length; i++){

                if (i == stateIntArray.length -1){
                    outWriter.append(String.valueOf(stateIntArray[i]));
                    outWriter.append("s,mapState:{Name:\"");
                    outWriter.append((String) getChildTag(blockIDCollection, String.valueOf(blockMap.get(stateIntArray[i])), StringTag.class).getValue());
                    outWriter.append("\"}}]");
                }else {
                    outWriter.append(String.valueOf(stateIntArray[i]));
                    outWriter.append("s,mapState:{Name:\"");
                    outWriter.append((String) getChildTag(blockIDCollection, String.valueOf(blockMap.get(stateIntArray[i])), StringTag.class).getValue());
                    outWriter.append("\"}},{mapSlot:");
                }
            }

            outWriter.append(",endPos:{X:");
            outWriter.append(String.valueOf(length));
            outWriter.append(",Y:");
            outWriter.append(String.valueOf(height));
            outWriter.append(",Z:");
            outWriter.append(String.valueOf(width));
            outWriter.append("}}");


            outWriter.close();




        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static void main(String args[]) {
        new SchemToGadget();
    }

    //does the bit shifting to create the relative positions of blocks used by BuildingGadgets
    public  int relPosToInt(int x, int y, int z) {
        int px = ((x & 0xff) << 16);
        int py = ((y & 0xff) << 8);
        int pz = ((z & 0xff));
        int p = (px + py + pz);
        return p;
    }
}