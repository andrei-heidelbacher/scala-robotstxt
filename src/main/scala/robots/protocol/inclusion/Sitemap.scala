package robots.protocol.inclusion

import java.net.URL

import scala.util.Try
import scala.xml.XML

/**
 * Abstract class that represents a sitemap.
 *
 * @author andrei
 */
sealed abstract class Sitemap {
  /**
   * The location (URL) of the sitemap.
   */
  val location: URL

  /**
   * All valid links at which the sitemap points to. A link is valid if it has
   * correct URL syntax and points to a page placed somewhere inside the
   * directory where the sitemap is located. It filters the URLs returned by the
   * `parseLinks` method.
   */
  final val links: Seq[URL] = filterURLs(parseLinks)

  /**
   * Returns all raw-URLs parsed from the sitemap.
   */
  protected def parseLinks: Seq[String]

  private def filterURLs(rawURLs: Seq[String]): Seq[URL] = {
    val rootDirectory = location.toString.reverse.dropWhile(_ != '/').reverse

    def makeURLs(urls: Seq[String]): Seq[URL] =
      urls.map(url => Try(new URL(url))).filter(_.isSuccess).map(_.get)

    def validURL(url: URL): Boolean =
      url.toString.startsWith(rootDirectory)

    makeURLs(rawURLs).filter(validURL)
  }
}

/**
 * Class that represents .xml sitemaps.
 */
final class SitemapXML(val location: URL, content: String) extends Sitemap {
  protected def parseLinks: Seq[String] =
    (XML.loadString(content) \ "url" \ "loc").map(_.text)
}

/**
 * Class that represents .rss sitemaps.
 */
final class SitemapRSS(val location: URL, content: String) extends Sitemap {
  protected def parseLinks: Seq[String] =
    (XML.loadString(content) \ "channel" \ "item" \ "link").map(_.text)
}

/**
 * Class that represents .txt sitemaps.
 */
final class SitemapTXT(val location: URL, content: String) extends Sitemap {
  protected def parseLinks: Seq[String] = content.split("\\s")
}

/**
 * Factory object for the [[robots.protocol.inclusion.Sitemap]] class.
 */
object Sitemap {
  /**
   * Automatically recognizes the sitemap format by returning the one which
   * returns the most valid links. If a new sitemap format is added, it should
   * also be updated in this method.
   *
   * @param location URL of the sitemap
   * @param content Raw string content of the sitemap
   * @return Concetre [[robots.protocol.inclusion.Sitemap]] automatically
   * identified according to the raw content.
   */
  def apply(location: URL, content: String): Sitemap = {
    val xml = Try(new SitemapXML(location, content))
    val rss = Try(new SitemapRSS(location, content))
    val txt = Try(new SitemapTXT(location, content))
    val sitemaps = Seq(xml, rss, txt)
    sitemaps.filter(_.isSuccess).map(_.get).maxBy(_.links.length)
  }
}