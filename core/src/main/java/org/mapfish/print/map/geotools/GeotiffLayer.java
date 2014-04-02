/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.map.geotools;

import com.google.common.base.Optional;
import com.google.common.io.Resources;
import jsr166y.ForkJoinPool;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.styling.Style;
import org.mapfish.print.config.Template;
import org.mapfish.print.json.PJsonObject;
import org.mapfish.print.map.MapLayerFactoryPlugin;
import org.mapfish.print.map.style.StyleParser;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;

import static org.mapfish.print.Constants.RASTER_STYLE_NAME;

/**
 * Reads a Geotiff file from a URL.
 *
 * @author Jesse on 3/26/14.
 */
public final class GeotiffLayer extends AbstractGridCoverage2DReaderLayer {

    /**
     * Constructor.
     *
     * @param reader          the reader to use for reading the geotiff.
     * @param style           style to use for rendering the data.
     * @param executorService the thread pool for doing the rendering.
     */
    public GeotiffLayer(final GeoTiffReader reader, final Style style, final ExecutorService executorService) {
        super(reader, style, executorService);
    }

    /**
     * Parser for creating {@link org.mapfish.print.map.geotools.GeotiffLayer} layers from request data.
     */
    public static final class Plugin implements MapLayerFactoryPlugin {

        private static final String TYPE = "geotiff";
        private static final String URL = "url";

        @Autowired
        private StyleParser parser;
        @Autowired
        private ForkJoinPool forkJoinPool;


        @Nonnull
        @Override
        public Optional<GeotiffLayer> parse(final Template template, @Nonnull final PJsonObject layerJson) throws IOException {
            final Optional<GeotiffLayer> result;
            final String type = layerJson.getString("type");
            final String geotiffUrl = layerJson.optString(URL);
            if (TYPE.equalsIgnoreCase(type) && geotiffUrl != null) {

                final String styleRef = layerJson.optString("style", RASTER_STYLE_NAME);

                Style style = template.getStyle(styleRef)
                        .or(this.parser.loadStyle(template.getConfiguration(), styleRef))
                        .or(template.getConfiguration().getDefaultStyle(RASTER_STYLE_NAME));

                GeoTiffReader geotiffReader = getGeotiffReader(template, geotiffUrl);
                result = Optional.of(new GeotiffLayer(geotiffReader, style, this.forkJoinPool));
            } else {
                result = Optional.absent();
            }
            return result;
        }

        private GeoTiffReader getGeotiffReader(final Template template, final String geotiffUrl) throws IOException {
            URL url = new URL(geotiffUrl);
            final String protocol = url.getProtocol();
            final File geotiffFile;
            if (protocol.equalsIgnoreCase("file")) {
                geotiffFile = new File(template.getConfiguration().getDirectory(), geotiffUrl.substring("file://".length()));
                assertFileIsInConfigDir(template, geotiffFile);
            } else {
                geotiffFile = File.createTempFile("downloadedGeotiff", ".tiff");
                OutputStream output = null;
                try {
                    output = new FileOutputStream(geotiffFile);
                    Resources.copy(url, output);
                } finally {
                    if (output != null) {
                        output.close();
                    }
                }
            }

            final GeoTiffReader reader = new GeoTiffFormat().getReader(geotiffFile);
            return reader;
        }

        private void assertFileIsInConfigDir(final Template template, final File file) {
            final String configurationDir = template.getConfiguration().getDirectory().getAbsolutePath();
            if (!file.getAbsolutePath().startsWith(configurationDir)) {
                throw new IllegalArgumentException("The geoJson attribute is a file url but indicates a file that is not within the" +
                                                   " configurationDirectory: " + file.getAbsolutePath());
            }
        }

    }
}
