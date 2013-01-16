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

import de.hopp.generator.IOHandler;

// this would be so much easier with Java 7 ...
public class Files {
    
    /**
     * Copies a resource into another directory.
     * @param resource resource string (may point to a file or directory)
     * @param out output file, into which the resource should be copied (has to be a directory)
     * @throws IOException file operations cause IOExceptions, naturally...
     */
    public static void copy(String resource, File out, IOHandler IO) throws IOException {
        // get the URL of the provided resource
        IO.debug("    looking for resource " + resource);
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        
        // if the URL is null, the resource is not available on this system
        if(url == null) throw new IOException("couldn't find resource " + resource);
        IO.debug("    found resource at " + url + ". Now copying ...");
        
        // construct a file from the url to check if it's inside a jar archive
        File in = new File(url.getPath());
        
        if(in.exists()) {
            // if it exists, just copy it
            copy(in, out, IO);
        } else {
            // if the file pointing to the resource doesn't exist, it probably is inside a jar file
            IO.debug("    resource seems to point inside a jar file");
            
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
                        IO.debug("    found and ignored directory " + entry.getName());
                    } else {
                        // TODO cut away everything above the "resource" in a more elegant way...
                        String path = entry.getName().substring(entry.getName().indexOf(resource) + resource.length() + 1);
                        
                        File targetFile = new File(out, path);
                        
                        // create parent directories
                        targetFile.getParentFile().mkdirs();

                        IO.verbose("    copying file " + entry.getName() +
                                " to " +  targetFile.getPath());
                        // copy the resource from the jar file
                        copyToStream(jarFile.getInputStream(entry), new FileOutputStream(targetFile));
                    }
                }
            } finally {
                jarFile.close();
            }
        } 
    }
    
    /**
     * Copies files and directories into another directory.
     * @param in the input file (maybe a directory or file, but has to exist.)
     * @param out the output file (has to be a directory)
     * @throws IOException file operations cause IOExceptions, naturally...
     */
    public static void copy(File in, File out, IOHandler IO) throws IOException {
        
        // if the input file doesn't exist, throw an exception
        if(! in.exists()) throw new IOException("Input path " + in.getPath() + " doesn't exist");

        // if verbose print message
//        IO.verbose("  copying: " + in.getPath());
        
        if(in.isDirectory()) {
            // if the input file is a directory create the corresponding target directory...
            if(! out.exists()) out.mkdirs();
            
            // ... and copy all its contents
            for(String s : in.list()) copy(new File(in, s), new File(out, s), IO);
            
        } else {
            IO.verbose("    copying file " + in.getPath() + " to " + out.getPath());
            
            // otherwise just copy the file using streams
            FileInputStream   fin = new FileInputStream(in);
            FileOutputStream fout = new FileOutputStream(out);
            
            try {
                copyToStream(new FileInputStream(in), new FileOutputStream(out));
            } finally {
                fin.close(); fout.close();
            }
        }
    }
    
    /**
     * Copies data from one stream to another, till input stream is at an end.
     * @param in the in stream.
     * @param out the out stream.
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
}
