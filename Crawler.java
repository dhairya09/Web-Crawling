import com.google.common.io.Files;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

public class Crawler extends WebCrawler {
    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|json|mp3|mp3|zip|gz))$");

    CrawlState crawlState;

    public Crawler() {
        crawlState = new CrawlState();
    }

    private static File storageFolder;

    public static void configure(String storageFolderName) {
        storageFolder = new File(storageFolderName);
        if (!storageFolder.exists()) {
            storageFolder.mkdirs();
        }
    }

    @Override
    public boolean shouldVisit(Page page, WebURL url) {
        String hrefw = url.getURL().toLowerCase();
        String href=hrefw.replaceAll(",", "-");
        String type = "";
        if ((href.startsWith("https://www.washingtonpost.com/"))|| (href.startsWith("https://www.washingtonpost.com/"))) {
            type = "OK";
        } else {
            type = "N_OK";
        }
        crawlState.discoveredUrls.add(new UrlInfo(href, type));
        return !FILTERS.matcher(href).matches() && type.equals("OK");
    }

    @Override
    public void visit(Page page) {
        String urlw = page.getWebURL().getURL();
        String url=urlw.replaceAll(",", "-");
        String contentType = page.getContentType().split(";")[0];
        ArrayList<String> outgoingUrls = new ArrayList<String>();

        UrlInfo urlInfo;
        if (contentType.equals("text/html")) { // html
            if (page.getParseData() instanceof HtmlParseData) {
                HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
                Set<WebURL> links = htmlParseData.getOutgoingUrls();
                for (WebURL link : links) {
                    outgoingUrls.add(link.getURL());
                }
                urlInfo = new UrlInfo(url, page.getContentData().length, outgoingUrls, "text/html", ".html");
                crawlState.visitedUrls.add(urlInfo);
            } else {
                urlInfo = new UrlInfo(url, page.getContentData().length, outgoingUrls, "text/html", ".html");
                crawlState.visitedUrls.add(urlInfo);
            }
        } else if (contentType.equals("application/msword")) { // doc
            urlInfo = new UrlInfo(url, page.getContentData().length, outgoingUrls, "application/msword", ".doc");
            crawlState.visitedUrls.add(urlInfo);
        } else if (contentType.equals("application/pdf")) { // pdf
            urlInfo = new UrlInfo(url, page.getContentData().length, outgoingUrls, "application/pdf", ".pdf");
            crawlState.visitedUrls.add(urlInfo);
        } else if (contentType.contains("image/")) { // images
        	String[] extension = contentType.split("/");
            urlInfo = new UrlInfo(url, page.getContentData().length, outgoingUrls, contentType, extension[1]);
            crawlState.visitedUrls.add(urlInfo);
        } else if (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            urlInfo = new UrlInfo(url, page.getContentData().length, outgoingUrls, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");
            crawlState.visitedUrls.add(urlInfo);
        } else {
            urlInfo = new UrlInfo(url, page.getContentData().length, outgoingUrls, "unknown", "");
            crawlState.visitedUrls.add(urlInfo);
        }

        if (!urlInfo.extension.equals("")) {
            String filename = storageFolder.getAbsolutePath() + "/" + urlInfo.hash + urlInfo.extension;
            try {
                Files.write(page.getContentData(), new File(filename));
            } catch (IOException iox) {
                System.out.println("Failed to write file: " + filename);
            }
        }
    }

    @Override
    protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {
        crawlState.attemptUrls.add(new UrlInfo(webUrl.getURL(), statusCode));
    }

    @Override
    public Object getMyLocalData() {
        return crawlState;
    }
}