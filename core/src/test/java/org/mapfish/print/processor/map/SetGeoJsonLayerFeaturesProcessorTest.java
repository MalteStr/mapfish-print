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

package org.mapfish.print.processor.map;

import jsr166y.ForkJoinPool;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.mapfish.print.config.Template;
import org.mapfish.print.output.Values;
import org.mapfish.print.parser.MapfishParser;
import org.mapfish.print.util.ImageSimilarity;
import org.mapfish.print.wrapper.json.PJsonObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Basic test of the set features to vector layers processor.
 * <p/>
 * Created by Stéphane Brunner on 16/4/14.
 */
public class SetGeoJsonLayerFeaturesProcessorTest extends AbstractMapfishSpringTest {
    private static final String BASE_DIR = "setfeaturesprocessor/";

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private MapfishParser parser;
    @Autowired
    private ForkJoinPool forkJoinPool;

    @Test
    public void testExecute() throws Exception {
        final Configuration config = configurationFactory.getConfig(getFile(BASE_DIR + "config.yaml"));
        final Template template = config.getTemplate("main");
        PJsonObject requestData = loadJsonRequestData();
        Values values = new Values(requestData, template, this.parser);

        this.forkJoinPool.invoke(template.getProcessorGraph().createTask(values));

        BufferedImage map = values.getObject("map", BufferedImage.class);
        new ImageSimilarity(map, 2).assertSimilarity(getFile(BASE_DIR + "expectedSimpleImage.tiff"), 0);
    }

    public static PJsonObject loadJsonRequestData() throws IOException {
        return parseJSONObjectFromFile(CreateMapProcessorFixedScaleBBoxGeoJsonTest.class, BASE_DIR + "requestData.json");
    }

}
