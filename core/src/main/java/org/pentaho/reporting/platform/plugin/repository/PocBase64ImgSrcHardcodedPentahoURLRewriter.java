/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.repository;

import org.pentaho.reporting.engine.classic.core.modules.output.table.html.URLRewriteException;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.URLRewriter;
import org.pentaho.reporting.libraries.repository.ContentEntity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Proof fo Concept class to test out embedding base64 images in <code>&#60img&#62;</code>
 */
public class PocBase64ImgSrcHardcodedPentahoURLRewriter implements URLRewriter  {

  URLRewriter deletgateUrlRewriter;

  /**
   * String format
   * <br/>
   * Image Examples:
   * <pre>
   * &#60;img src="data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAWgBaAAD/4gxYSUNDX1BST0ZJTEUAAQEAAAxITGlubwIQAAB..." /&#62;
   *</pre>
   *
   * Other Examples:
   * <pre>
   * &#60;link rel="stylesheet" type="text/css" href="data:text/css;base64,/9j/4AAQSkZJRgABAQEAWgBaAAD/4gxYSUNDX1BST0ZJTEUAAQEAAAxITGlubwIQAAB..." /&#62;
   *
   * &#60;script type="text/javascript" src="data:text/javascript;base64,/9j/4AAQSkZJRgABAQEAWgBaAAD/4gxYSUNDX1BST0ZJTEUAAQEAAAxITGlubwIQAAB..."&#62;&#60;/script&#62;
   * </pre>
   *
   * @see <a href="https://www.freeformatter.com/base64-encoder.html">https://www.freeformatter.com/base64-encoder.html</a>.
   */
  //TODO handle all image types, png, jpeg, gif ... see DefaultHtmlContentGenerator#isSupportedImageFormat and look into getting 'image/xyz' from DefaultHtmlContentGenerator#queryMimeType or ImageData#getMimeType
  private static final String HTML_IMG_SRC_BASE64_FORMAT = "data:image/png;base64,%0$s";

  private static final String TEST_IMAGE_PICTURE_1_BASE64_TXT = "picture_1_base64.txt";

  private static final String TEST_IMAGE_PICTURE_2_BASE64_TXT = "picture_2_base64.txt";
  int imageCounter = 0;
  public PocBase64ImgSrcHardcodedPentahoURLRewriter( final URLRewriter urlRewriter ) {
    deletgateUrlRewriter = urlRewriter;
  }

  public String rewrite( final ContentEntity contentEntry, final ContentEntity dataEntity ) throws URLRewriteException {

    if ( dataEntity.getName().contains( "picture" ) ) {
      String filenameTestImageBase64 =
          ( imageCounter++ == 0 ) ? TEST_IMAGE_PICTURE_1_BASE64_TXT : TEST_IMAGE_PICTURE_2_BASE64_TXT;
      /**
       * TODO get image InputStream or byte[] see this function is called from DefaultHtmlContentGenerator#writeImage - https://github.com/pentaho/pentaho-reporting/blob/48c3d89b4e7a808a9a943d878f57009e81b1e002/engine/core/src/main/java/org/pentaho/reporting/engine/classic/core/modules/output/table/html/helper/DefaultHtmlContentGenerator.java#L256-L266
       *
       * Possible options:
       * Option 1 - reorder DefaultHtmlContentGenerator#writeImage + call (ReportContentItem)dataFile.getInputStream
       * before
       * <pre>
       *           ContentItem dataFile = this.dataLocation.createItem(this.dataNameGenerator.generateName(filename, data.getMimeType())); // just JRC repository location ie "node /pentaho/tenant0/home/admin/report/picture1708945689.png" no data yet
       *           String contentURL = this.rewriterService.rewriteContentDataItem(dataFile); // just the HTML src attribute snippet
       *           OutputStream out = new BufferedOutputStream(dataFile.getOutputStream()); // JCR repo stream to write too
       *
       *           try {
       *             out.write(data.getImageData()); // now there is data here at JCR "node /pentaho/tenant0/home/admin/report/picture1708945689.png"
       *             out.flush();
       *           } finally {
       *             out.close();
       *           }
       * </pre>
       * after
       * <pre>
       *           ContentItem dataFile = this.dataLocation.createItem(this.dataNameGenerator.generateName(filename, data.getMimeType()));
       *           OutputStream out = new BufferedOutputStream(dataFile.getOutputStream());
       *
       *           try {
       *             out.write(data.getImageData());
       *             out.flush();
       *           } finally {
       *             out.close();
       *           }
       *           String contentURL = this.rewriterService.rewriteContentDataItem(dataFile);
       * </pre>
       * Then we would just have to call
       * <pre>
       *   InputStream isTestImageBas64 = (ContentItem)dataEntity.getInputStream();
       * </pre>
       *
       */
      InputStream isTestImageBas64 = this.getClass().getClassLoader().getResourceAsStream( filenameTestImageBase64 );

      if ( isTestImageBas64 == null ) throw new URLRewriteException( "test image '" + filenameTestImageBase64 + "' could not be found" );

      /**
       * TODO convert raw image files that are served from 'http://localhost:8080/pentaho/api/repo/files/%3Ahome%3Aadmin%3Apicture157310399.png/inline'
       * to base64.
       *
       * see ReportContentLocation#createItem(String) to access image from repository
       *
       * Base64 encode:
       *  - https://stackoverflow.com/questions/13109588/encoding-as-base64-in-java
       *  -- since java 8:  java.util.Base64.getEncoder().encode("Test".getBytes());
       *  --- https://docs.oracle.com/javase/8/docs/api/java/util/Base64.html
       *  - https://www.baeldung.com/java-base64-image-string
       *  -- no need to depend on another library
       */
      String textTestImageBas64 = new BufferedReader(
          new InputStreamReader( isTestImageBas64, StandardCharsets.UTF_8 ) )
          .lines()
          .collect( Collectors.joining( "" ) );

      /**
       * TODO if doing <img src="data:image/png;base64,{base64_image}...> then no need to save image in repo
       *  see DefaultHtmlContentGenerator#writeImage - https://github.com/pentaho/pentaho-reporting/blob/48c3d89b4e7a808a9a943d878f57009e81b1e002/engine/core/src/main/java/org/pentaho/reporting/engine/classic/core/modules/output/table/html/helper/DefaultHtmlContentGenerator.java#L263-L266
       */
      return String.format( HTML_IMG_SRC_BASE64_FORMAT, textTestImageBas64 );

    } else {
      // default to normal PentahoURLRewriter behavior for css and other items
      return deletgateUrlRewriter.rewrite( contentEntry, dataEntity );
    }
  }
}
