package de.dosmike.sponge.oreget.utils;

import de.dosmike.sponge.oreget.multiplatform.PlatformProbe;
import de.dosmike.sponge.oreget.multiplatform.SharedInstances;
import de.dosmike.sponge.oreget.oreapi.OreApiV2;
import de.dosmike.sponge.oreget.oreapi.v2.OreProject;
import de.dosmike.sponge.oreget.oreapi.v2.OreVersion;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public class PluginDownloader extends Thread implements Runnable {

    long file_loaded=0L;
    OreProject project;
    OreVersion version;
    boolean success=false;
    boolean done=false;
    public PluginDownloader(OreProject project, OreVersion version) {
        this.project = project;
        this.version = version;
    }
    Path localFile=null;

    /** TODO display a disclaimer before downloading a file! (To be done somewhere before this) */
    @Override
    public void run() {
        URL targetUrl = SharedInstances.getOre().getDownloadURL(project.getNamespace(), version);
        if (targetUrl==null) {
            done = true;
            return;
        }
        OutputStream fos=null;
        InputStream in=null;
        try {
            HttpsURLConnection connection = (HttpsURLConnection) targetUrl.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
//            connection.setRequestProperty("Accept", "application/octet-stream");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("User-Agent", "OreGet (by DosMike)/1.0");
            SharedInstances.getOre().getSession().authenticate(connection);
            connection.setDoInput(true);
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 400) {
                OreApiV2.tryPrintErrorBody(connection);
                return;
            }
            //extract filname from content-disposition header
            String filename = connection.getHeaderFields().entrySet().stream()
                    .filter((entry)->"content-disposition".equalsIgnoreCase(entry.getKey()))
                    .map(entry->entry.getValue().stream()
                            .filter(v->v.contains("filename="))
                            .findFirst()
                            .map(header->{
                                String[] parts = header.split(";");
                                for (String part : parts) {
                                    int index = part.indexOf("filename=");
                                    if (index >= 0)
                                        return part.substring(index+9);
                                }
                                return "";
                            })
                            .orElse(""))
                    .findFirst().orElse("");
            if (filename.isEmpty()) return;
            if (filename.startsWith("\"") && filename.endsWith("\""))
                filename = filename
                        //strip quotes
                        .substring(1, filename.length()-1)
                        //unescape quoted-pairs as noticed in https://www.ietf.org/rfc/rfc2616.txt
                        //  -> 4.2 Message Headers  -> 2.2 Basic Rules  -> quoted-string
                        .replaceAll("\\\\(.)", "$1");

            localFile = PlatformProbe.getCacheDirectory().resolve(filename);
            fos = Files.newOutputStream(localFile);
            in = connection.getInputStream();
//            Base64.getDecoder().wrap(in);
            MessageDigest hasher = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024]; int r;
            while ((r=in.read(buffer))>0) {
                fos.write(buffer, 0, r);
                hasher.update(buffer, 0, r);
                file_loaded += r;
            }
            byte[] rawhash = hasher.digest();
            StringBuilder hashBuilder = new StringBuilder();
            for (byte b : rawhash)
                hashBuilder.append(String.format("%02X", ((int)b&0xFF)));

            success = hashBuilder.toString().equalsIgnoreCase(version.getFileMD5());
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {
            done = true;
            try { fos.flush(); } catch (Exception ignore) {}
            try { fos.close(); } catch (Exception ignore) {}
            try { in.close(); } catch (Exception ignore) {}
        }
    }

    /** @return the downloaded file on success */
    public Optional<Path> target() {
        return success ? Optional.ofNullable(localFile) : Optional.empty();
    }

    public float getProgress() {
        return (float)file_loaded/version.getFileSize();
    }
    public boolean isDone() {
        return done;
    }

}
