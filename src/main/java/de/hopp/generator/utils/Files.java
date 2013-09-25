package de.hopp.generator.utils;

import static org.apache.commons.io.FileUtils.contentEquals;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.io.FileUtils.openInputStream;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.write;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import de.hopp.generator.IOHandler;

/**
 * A utility class for resource location and deployment.
 *
 * Build upon the Apache Commons IO package and Java resource
 * functionality, this class provides methods for locating
 * resources in- and outside of .jar files and deploying them
 * incrementally.
 * This includes checking for existence of the files to be
 * deployed beforehand and not deploying the new file,
 * if the content of the existing file is identical.
 * This leaves timestamps of such files untouched and allows
 * incremental builds for processes working on the deployed files.
 *
 * @author Thomas Fischer
 */
public class Files {

    /**
     * Copies a file iff the target file does not exist
     * or has different content than the source file.
     *
     * @param srcFile file to be copied.
     * @param destFile target to where the file should be copied to.
     * @param IO IOHandler for debug messages.
     * @return true if the file needed to be copied, false otherwise
     *      (i.e. identical file already exists at target).
     * @throws IOException in case of an I/O error
     */
    public static boolean deploy(File srcFile, File destFile, IOHandler IO) throws IOException {
        IO.verbose("    copying " + srcFile.getPath() + " to " + destFile.getPath());

        if(! srcFile.exists()) throw new IOException("Input path " + srcFile.getPath() + " doesn't exist");

        if(srcFile.isDirectory()) {
            IO.debug("      descending into directory");

            // if the file is a directory, create it ...
            if(! destFile.exists()) destFile.mkdirs();

            boolean newFiles = false;

            // ... and copy all its contents
            for(String s : srcFile.list())
                newFiles = newFiles || deploy(new File(srcFile, s), new File(destFile, s), IO);

            return newFiles;
        }

        // otherwise check if the file already exists and is identical to the source
        if(destFile.exists() && contentEquals(srcFile, destFile)) {
            IO.debug("      skipping since identical file already exists in target directory");
            return false;
        }

        // if not, (re-)deploy the file
        copyFile(srcFile, destFile);
        return true;
    }

    /**
     * Copies a resource iff the target file does not exist
     * or has different content than the source file.
     *
     * @param srcPath path to the resource to be copied.
     * @param destFile target to where the file should be copied to.
     * @param IO IOHandler for debug messages.
     * @return true if the file needed to be copied, false otherwise
     *      (i.e. identical file already exists at target).
     * @throws IOException in case of an I/O error
     */
    public static boolean deploy(String srcPath, File destFile, IOHandler IO) throws IOException {
        IO.verbose("    copying " + srcPath + " to " + destFile.getCanonicalPath());

        // get the URL of the provided resource
        URL srcURL = getResource(srcPath, IO);

        // construct a file from the url to check if it's inside a jar archive
        File in = new File(srcURL.getPath());

        if(in.exists()) {
            // if it exists, just copy it
            return deploy(in, destFile, IO);
        } else {
            // if the file pointing to the resource doesn't exist, it probably is inside a jar file
            IO.debug("    resource not found as file - checking if it points inside a jar file");
            boolean newFiles = false;


            // get the jar file, where the resource is contained
            JarFile jarFile = ((JarURLConnection)srcURL.openConnection()).getJarFile();
            IO.debug("    iterating over jar file contents");

            try {
                // get all contained entries
                Enumeration<JarEntry> entries = jarFile.entries();

                // iterate over each entry
                while(entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    // if it isn't a subpath of the given resource, skip this entry
                    if (! entry.getName().contains(srcPath)) continue;

                    if(entry.isDirectory()) {
                        IO.debug("      found and ignored directory " + entry.getName());
                    } else {

                        // cut away the path until <resource> (including <resource>)
                        String path = entry.getName().substring(entry.getName().indexOf(srcPath) + srcPath.length());

                        // prepend target directory
                        File target = new File(destFile, path);

                        IO.verbose("      copying file " + entry.getName() + " to " +  target.getPath());
                        if(compareContents(jarFile.getInputStream(entry), target)) {
                            IO.debug("      skipping since identical file already exists in target directory");
                        } else {
                            copyInputStreamToFile(jarFile.getInputStream(entry), target);
                            newFiles = true;
                        }
                    }
                }

                return newFiles;
            } finally {
                jarFile.close();
            }
        }

    }

    /**
     * Determines if the content of an InputStream matches the content of a file.
     * If the file does not exist, it does NOT match, regardless of the stream contents.
     *
     * @param srcStream stream to be compared.
     * @param destFile file to be compared.
     * @return true if the contents are equal, false otherwise
     * @throws IOException in case of an I/O error
     */
    private static boolean compareContents(InputStream srcStream, File destFile) throws IOException {
        if(!destFile.exists()) return false;

        InputStream destStream = openInputStream(destFile);
        // read the first character of both streams
        int a = srcStream.read();
        int b = destStream.read();

        while(a == b && a != -1) {
            a = srcStream.read();
            b = destStream.read();
        }

        if (a == -1 && b == -1) return true;
        else return false;
    }

    /**
     * Writes a string to a file iff the target file does not exist
     * or has different content than the string.
     *
     * @param content String to be written to a file.
     * @param destFile target file where the string should be written to.
     * @param IO IOHandler for debug messages.
     * @return true if something was written, false otherwise (i.e. identical file already exists).
     * @throws IOException in case of an I/O error
     */
    public static boolean deployContent(String content, File destFile, IOHandler IO) throws IOException {
        IO.verbose("    deploying " + destFile.getPath());
        if(destFile.exists() && content.equals(readFileToString(destFile))) {
            IO.debug("      skipping since identical file already exists in target directory");
            return false;
        }

        write(destFile, content);
        return true;
    }

    /**
     * Writes the content of a buffer to a file iff the target file does not exist
     * or has different content than the buffer.
     *
     * @param content String to be written to a file.
     * @param destFile target file where the string should be written to.
     * @param IO IOHandler for debug messages.
     * @return true if something was written, false otherwise (i.e. identical file already exists).
     * @throws IOException in case of an I/O error
     */
    public static boolean deployContent(StringBuffer content, File destFile, IOHandler IO) throws IOException {
        return deployContent(content.toString(), destFile, IO);
    }

    /**
     * Selects the URL corresponding to a provided resource string.
     * @param resource Resource string (may point to a file or directory).
     * @param IO IOHandler for debug output.
     * @return The found URL. Cannot be null.
     * @throws IOException in case of an I/O error
     */
    public static URL getResource(String resource, IOHandler IO) throws IOException {
        // get the URL of the provided resource
        IO.debug("    acquiring resource " + resource);
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);

        // if the URL is null, the resource is not available on this system
        if(url == null) throw new IOException("couldn't find resource " + resource);
        IO.debug("    found resource at " + url);

        return url;
    }

//    /**
//     * Copies a resource into another directory.
//     * @param resource Resource string (may point to a file or directory).
//     * @param targetFile Output file, into which the resource should be copied.
//     * @param IO IOHandler for debug output.
//     * @throws IOException file operations cause IOExceptions, naturally...
//     */
//    public static void copy(String resource, File targetFile, IOHandler IO) throws IOException {
//        URL url = getResource(resource, IO);
//
//        IO.debug("    now copying ...");
//        // construct a file from the url to check if it's inside a jar archive
//        File in = new File(url.getPath());
//
//        if(in.exists()) {
//            // if it exists, just copy it
//            copy(in, targetFile, IO);
//        } else {
//            // if the file pointing to the resource doesn't exist, it probably is inside a jar file
//            IO.debug("    resource seems to point inside a jar file");
//
//            // get the jar file, where the resource is contained
//            JarFile jarFile = ((JarURLConnection)url.openConnection()).getJarFile();
//
//            try {
//                // get all contained entries
//                Enumeration<JarEntry> entries = jarFile.entries();
//
//                // iterate over each entry
//                while(entries.hasMoreElements()) {
//                    JarEntry entry = entries.nextElement();
//
//                    // if it isn't a subpath of the given resource, skip this entry
//                    if (! entry.getName().contains(resource)) continue;
//
//                    if(entry.isDirectory()) {
//                        IO.debug("    found and ignored directory " + entry.getName());
//                    } else {
//
//                        // cut away the path until <resource> (including <resource>)
//                        String path = entry.getName().substring(entry.getName().indexOf(resource) + resource.length());
//
//                        // prepend target directory
//                        File target = new File(targetFile, path);
//
//                        // create parent directories
//                        target.getParentFile().mkdirs();
//
//                        // copy the resource from the jar file
//                        IO.verbose("    copying file " + entry.getName() + " to " +  target.getPath());
//                        copyInputStreamToFile(jarFile.getInputStream(entry), target);
//                    }
//                }
//            } finally {
//                jarFile.close();
//            }
//        }
//    }
//
//    /**
//     * Copies files and directories into another directory.
//     * @param in the input file (maybe a directory or file)
//     * @param out the output file
//     * @param IO IOHandler of this generator run
//     * @throws IOException file operations cause IOExceptions, naturally...
//     */
//    private static void copy(File in, File out, IOHandler IO) throws IOException {
//
//        // if the input file doesn't exist, throw an exception
//        if(! in.exists()) throw new IOException("Input path " + in.getPath() + " doesn't exist");
//
//        // if verbose print message
////        IO.verbose("  copying: " + in.getPath());
//
//        // create the parent folder, if it doesn't exist
//        if(out.getParentFile() != null && ! out.getParentFile().exists()) out.getParentFile().mkdirs();
//
//        if(in.isDirectory()) {
//            // if the file is a directory, create it ...
//            if(! out.exists()) out.mkdirs();
//
//            // ... and copy all its contents
//            for(String s : in.list()) copy(new File(in, s), new File(out, s), IO);
//
//        } else {
//            IO.verbose("    copying file " + in.getPath() + " to " + out.getPath());
//
//            copyFile(in, out);
//        }
//    }
}
