package de.hopp.generator.utils;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import de.hopp.generator.IOHandler;

public class Files {
    
    /**
     * Copies a resource into another directory.
     * @param resource resource string (may point to a file or directory)
     * @param out output file, into which the resource should be copied
     * @throws IOException file operations cause IOExceptions, naturally...
     */
    public static void copy(String resource, File targetFile, IOHandler IO) throws IOException {
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
            copy(in, targetFile, IO);
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
                    
                    if(entry.isDirectory()) {
                        IO.debug("    found and ignored directory " + entry.getName());
                    } else {
                        
                        // cut away the path until <resource> (including <resource>)
                        String path = entry.getName().substring(entry.getName().indexOf(resource) + resource.length());
                        
                        // prepend target directory
                        File target = new File(targetFile, path);
                        
                        // create parent directories
                        target.getParentFile().mkdirs();

                        // copy the resource from the jar file
                        IO.verbose("    copying file " + entry.getName() + " to " +  target.getPath());
                        copyInputStreamToFile(jarFile.getInputStream(entry), target);
                    }
                }
            } finally {
                jarFile.close();
            }
        } 
    }
    
    /**
     * Copies files and directories into another directory.
     * @param in the input file (maybe a directory or file)
     * @param out the output file
     * @throws IOException file operations cause IOExceptions, naturally...
     */
    private static void copy(File in, File out, IOHandler IO) throws IOException {
        
        // if the input file doesn't exist, throw an exception
        if(! in.exists()) throw new IOException("Input path " + in.getPath() + " doesn't exist");

        // if verbose print message
//        IO.verbose("  copying: " + in.getPath());
        
        // create the parent folder, if it doesn't exist
        if(! out.getParentFile().exists()) out.getParentFile().mkdirs();
        
        if(in.isDirectory()) {
            // if the file is a directory, create it ...
            if(! out.exists()) out.mkdirs();
            
            // ... and copy all its contents
            for(String s : in.list()) copy(new File(in, s), new File(out, s), IO);
            
        } else {
            IO.verbose("    copying file " + in.getPath() + " to " + out.getPath());
            
            copyFile(in, out);
        }
    }
}
