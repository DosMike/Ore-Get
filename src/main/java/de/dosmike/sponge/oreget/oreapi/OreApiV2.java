package de.dosmike.sponge.oreget.oreapi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.dosmike.sponge.oreget.oreapi.v2.*;
import de.dosmike.sponge.oreget.utils.CachingCollection;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Calls within this class initially block, but do not consider and Rate Limits. In order to prevent dos-ing the Ore
 * Servers it is encouraged to use the RateLimiter with <code>OreApi.waitFor(()->OreApi.Method)</code>.
 * If you want to perform bulk operations you should consider collecting the futures returned from
 * <code>OreApi.enqueue(()->OreApi.Method)</code> and passing your collection through
 * <code>RateLimiter.waitForAll(collection)</code>. The resulting collection will be a list, holding order
 * of your supplied collection if applicable.
 */
public class OreApiV2 implements AutoCloseable {

    private OreSession session;
    private RateLimiter limiter;
    public RateLimiter getRateLimiter() {
        return limiter;
    }
    /** shorthand for {@link RateLimiter#waitFor(Supplier)}.<br>
     * API calls return optionals, so does waitFor. This method automatically unboxes one optional */
    public <T> Optional<T> waitFor(Supplier<Optional<T>> task) {
        return limiter.waitFor(task).orElseGet(Optional::empty);
    }
    /** shorthand for {@link RateLimiter#enqueue(Supplier)} */
    public <T> Future<T> enqueue(Supplier<T> task) {
        return limiter.enqueue(task);
    }
    public OreApiV2() {
        session = new OreSession();
        limiter = new RateLimiter();
        limiter.start();
    }
    @Override
    public void close() throws Exception {
        limiter.halt();
    }

    private static final SimpleDateFormat timestampParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final SimpleDateFormat timestampParser2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    static {
        timestampParser.setLenient(true);
        timestampParser2.setLenient(true);
    }
    public static long superTimeParse(String time) {
        try {
            return timestampParser.parse(time).getTime();
        } catch (Exception ignore) {}
        try {
            return timestampParser2.parse(time).getTime();
        } catch (Exception ignore) {}
        throw new RuntimeException("Could not parse time \""+time+"\"");
    }
    private static JsonParser parser = new JsonParser();
    private static final Pattern paginationPattern = Pattern.compile("limit=[1-9][0-9]*&offset=[0-9]+");

    private static HttpsURLConnection createConnection(String endpoint) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL("https://ore.spongepowered.org/api/v2"+endpoint).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "OreGet (by DosMike)/1.0");
        connection.setRequestProperty("Content-Type", "application/json");
        return connection;
    }
    private static JsonObject parseJson(HttpsURLConnection connection) throws IOException {
        return parser.parse(new InputStreamReader(connection.getInputStream())).getAsJsonObject();
    }
    public OreSession getSession() {
        return session;
    }

    public boolean authenticate() {
        if (session.isValid()) return true;
        try {
            HttpsURLConnection connection = createConnection("/authenticate");
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 400) {
                tryPrintErrorBody(connection);
                return false;
            }
            session = new OreSession(parseJson(connection));
            return session.isValid();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @param pagination a string as returned by {@link OrePagination#getQueryPage}
     * @return empty if connection failed
     */
    public Optional<OreResultList<OreProject>> projectSearch(String queryString, @Nullable String pagination) {
        if (!authenticate())
            throw new IllegalStateException("Could not create API session");
        //don't want to cache a search
        limiter.takeRequest();
        try {
            String totalQuery = "/projects?q="+ URLEncoder.encode(queryString,"UTF-8");
            if (pagination != null) {
                if (!paginationPattern.matcher(pagination).matches())
                    throw new IllegalArgumentException("Invalid pagination string");
                totalQuery += "&"+pagination;
            }
            HttpsURLConnection connection = session.authenticate(createConnection(totalQuery));
            connection.setDoInput(true);
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 400) {
                tryPrintErrorBody(connection);
                return Optional.empty();
            }
            OreResultList<OreProject> resultList = new OreResultList<>(parseJson(connection), OreProject.class);
            for (OreProject p : resultList.getResult())
                cacheProject(p);
            return Optional.of(resultList);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * @return empty if the connection failed or no such plugin exists
     */
    public Optional<OreProject> getProject(String pluginId) {
        authenticate();
        Optional<OreProject> cache = getCachedProject(pluginId);
        if (cache.isPresent()) return cache;

        limiter.takeRequest();
        try {
            String totalQuery = "/projects/"+ URLEncoder.encode(pluginId, "UTF-8");
            HttpsURLConnection connection = session.authenticate(createConnection(totalQuery));
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 400) {
                if (connection.getResponseCode()!=404) tryPrintErrorBody(connection);
                return Optional.empty();
            }
            return Optional.of(cacheProject(new OreProject(parseJson(connection))));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * @return empty if the connection failed or no such plugin exists
     */
    public Optional<OreResultList<OreVersion>> listVersions(String pluginId, @Nullable String pagination) {
        authenticate();
        //can't think of an easy way to get the ResultList back from the version cache, so I won't bother
        limiter.takeRequest();
        try {
            String totalQuery = "/projects/"+ URLEncoder.encode(pluginId, "UTF-8")+"/versions";
            if (pagination != null) {
                if (!paginationPattern.matcher(pagination).matches())
                    throw new IllegalArgumentException("Invalid pagination string");
                totalQuery += "?"+pagination;
            }
            HttpsURLConnection connection = session.authenticate(createConnection(totalQuery));
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 400) {
                tryPrintErrorBody(connection);
                return Optional.empty();
            }
            OreResultList<OreVersion> resultList = new OreResultList<>(parseJson(connection), OreVersion.class);
            for (OreVersion v : resultList.getResult())
                cacheVersion(pluginId.toLowerCase(), v);
            return Optional.of(resultList);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * @return empty if the connection failed or no such plugin or version exists
     */
    public Optional<OreVersion> getVersion(String pluginId, String versionName) {
        authenticate();
        Optional<OreVersion> cache = getCachedVersion(pluginId, versionName);
        if (cache.isPresent()) return cache;
        limiter.takeRequest();
        try {
            String totalQuery = "/projects/"+ URLEncoder.encode(pluginId, "UTF-8")+"/versions/"+ URLEncoder.encode(versionName, "UTF-8");
            HttpsURLConnection connection = session.authenticate(createConnection(totalQuery));
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 400) {
                if (connection.getResponseCode()!=404) tryPrintErrorBody(connection);
                return Optional.empty();
            }
            oreVersionCache.get(pluginId);
            return Optional.of(cacheVersion(pluginId.toLowerCase(), new OreVersion(parseJson(connection))));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static void tryPrintErrorBody(HttpsURLConnection connection) {
        try {
            System.err.println("Error Body for response "+connection.getResponseCode()+": "+connection.getResponseMessage());
            InputStream in = connection.getErrorStream();
            if (in != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String line;
                while ((line = br.readLine()) != null)
                    System.err.println(line);
                br.close();
            }
        } catch (IOException ignore) {}
    }

    public URL getDownloadURL(OreNamespace namespace, OreVersion version) {
        authenticate();
        try {
            if (version.getReviewState().equals(OreReviewState.REVIEWED))
                return new URL("https://ore.spongepowered.org/"+
                        URLEncoder.encode(namespace.getOwner(), "UTF-8")+"/"+
                        URLEncoder.encode(namespace.getSlug(), "UTF-8")+"/versions/"+
                        URLEncoder.encode(version.getName(), "UTF-8")+"/download");

            // I'll just fetch the url here, since I prompt confirmation within PluginJob
            URL requestUrl = new URL("https://ore.spongepowered.org/"+
                    URLEncoder.encode(namespace.getOwner(), "UTF-8")+"/"+
                    URLEncoder.encode(namespace.getSlug(), "UTF-8")+"/versions/"+
                    URLEncoder.encode(version.getName(), "UTF-8")+"/confirm?api=true");
            HttpsURLConnection connection = session.authenticate((HttpsURLConnection) requestUrl.openConnection());
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "OreGet (by DosMike)/1.0");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 400) {
                tryPrintErrorBody(connection);
                return null;
            }
            JsonObject response = new JsonParser().parse(new InputStreamReader(connection.getInputStream())).getAsJsonObject();
            String string = response.get("url").getAsString();
            return new URL(string);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private CachingCollection<OreProject> oreProjectCache = new CachingCollection<>(5, TimeUnit.MINUTES);
    private OreProject cacheProject(OreProject project) {
        oreProjectCache.add(project);
        return project;
    }
    private Optional<OreProject> getCachedProject(String pluginId) {
        return oreProjectCache.stream().filter(e->e.getPluginId().equalsIgnoreCase(pluginId)).findFirst();
    }
    private Map<String, CachingCollection<OreVersion>> oreVersionCache = new HashMap<>();
    private OreVersion cacheVersion(String pluginId, OreVersion version) {
        CachingCollection<OreVersion> cache = oreVersionCache.get(pluginId.toLowerCase());
        if (cache == null) {
            cache = new CachingCollection<>(5, TimeUnit.MINUTES);
            oreVersionCache.put(pluginId.toLowerCase(), cache);
        }
        cache.add(version);
        return version;
    }
    private Optional<OreVersion> getCachedVersion(String pluginId, String versionName) {
        CachingCollection<OreVersion> collection = oreVersionCache.get(pluginId.toLowerCase());
        if (collection == null) return Optional.empty();
        else return collection.stream()
                .filter(v -> v.getName().equalsIgnoreCase(versionName))
                .findFirst();
    }

    public void exportState(OutputStream outputStream) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        oos.writeObject(oreProjectCache);
        oos.writeObject(oreVersionCache);
        oos.writeObject(session);
        oos.flush();
    }
    public void importState(InputStream inputStream) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(inputStream);
        oreProjectCache = (CachingCollection<OreProject>) ois.readObject();
        oreVersionCache = (Map<String, CachingCollection<OreVersion>>) ois.readObject();
        session = (OreSession) ois.readObject();
    }
}
