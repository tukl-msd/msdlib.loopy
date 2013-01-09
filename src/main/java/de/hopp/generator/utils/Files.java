package de.hopp.generator.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import de.hopp.generator.exceptions.ExecutionFailed;

// this would be so much easier with Java 7 ...
public class Files {
    
    /**
     * Copies data from one stream to another, till input stream is at an end
     * @param in the in stream
     * @param out the out stream
     * @throws java.io.IOException stream operations cause IOExceptions, naturally...
     */
    public static void copyToStream(final InputStream in, final OutputStream out) throws IOException {

        // how many bytes have been read to our byte buffer
        int bytesRead;
        byte[] buffer = new byte[16384];

        // read a first chunk
        bytesRead = in.read(buffer);

        // while there is some rest (read guarantees to deliver -1 only on end of data)
        while(bytesRead != -1) {

            // write chunk to other stream
            out.write(buffer, 0, bytesRead);

            // get a new chunk
            bytesRead = in.read(buffer);
        }

        // flush the output stream, to be sure the data was written
        out.flush();
    }
    
    public static void copy(String resource, File out) throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        if(url == null) {
            System.out.println("    ERROR: couldn't find resource " + resource);
            throw new ExecutionFailed();
        }
        
        File in = new File(url.getPath());
        
        if(in.exists()) {
            copy(in, out);
        } else {
            // if the file pointing to the resource doesn't exist, it probably is inside a jar file
            // TODO if verbose print message
            
            // get the jar file, where the resource is contained
            JarFile jarFile = ((JarURLConnection)url.openConnection()).getJarFile(); 

            try {
            
                // get all contained entries
                Enumeration<JarEntry> entries = jarFile.entries();
                
                // iterate over each entry
                while(entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    
                    // if it isn't a subpath of the given resource, skip this entry
                    if (! entry.getName().contains(resource)) continue; 
                    
                    // skip directories
                    if(entry.isDirectory()) {
                        // TODO if verbose print message...
                    } else {
                        // TODO cut away everything above the "resource" in a more elegant way...
                        String path = entry.getName().substring(entry.getName().indexOf(resource) + resource.length() + 1);
                        
                        File targetFile = new File(out, path);
                        
                        // create parent directories
                        targetFile.getParentFile().mkdirs();

                        // copy the resource from the jar file
                        copyToStream(jarFile.getInputStream(entry), new FileOutputStream(targetFile));
                    }
                }
            } catch (IOException e) {
                System.out.println("ERROR: " + e.getMessage());
                throw new ExecutionFailed();
            } finally {
                jarFile.close();
            }
        } 
    }
    
    /**
     * Copies files and directories
     * @param in
     * @param out
     * @throws IOException
     */
    public static void copy(File in, File out) throws IOException {
//        if(! in.exists()) throw new IOException("Input path " + in.getPath() + " doesn't exist");
        
        System.out.println("copying: " + in.getPath());
        
        if(! in.exists()) System.out.println("doesn't exist ):");
        
        if(in.isDirectory()) {
            if(! out.exists()) out.mkdirs();
            for(String s : in.list())
                copy(new File(in, s), new File(out, s));
        } else {
            FileInputStream   fin = new FileInputStream(in);
            FileOutputStream fout = new FileOutputStream(out);
            
            try {
                copyToStream(new FileInputStream(in), new FileOutputStream(out));
            } finally {
                fin.close(); fout.close();
            }
        }
    }
    
//    public static void copyDir(File in, File out) throws IOException {
//        if(! in.exists() || ! in.isDirectory()) throw new IOException();
//        if(out.exists() && ! out.isDirectory()) throw new IOException();
//        
//        for(String s : in.list()) {
//            File f = new File(in, s);
//            if(f.isDirectory()) {
//                // TODO these names will be too long - need only the last bit, after the last file separator
//                copy(f, new File(out, s));
//            } else if(f.isFile()) {
//                copy(f, new File(out, s));
//            } else {
//                // trollolo?
//            }
//        }
//    }
}
