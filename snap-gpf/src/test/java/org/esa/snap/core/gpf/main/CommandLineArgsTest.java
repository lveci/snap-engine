/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.gpf.main;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.runtime.Config;
import org.junit.Test;

import java.util.Map;
import java.util.SortedMap;

import static org.esa.snap.core.gpf.main.CommandLineArgs.*;
import static org.junit.Assert.*;

public class CommandLineArgsTest {

    private static final int K = 1024;
    private static final int M = 1024 * 1024;

    @Test
    public void testSystemProperties() throws Exception {
        CommandLineArgs lineArgs = parseArgs("Reproject", "-Dfoo=bar", "-Dbeam.performance=lightning");
        Map<String, String> systemPropertiesMap = lineArgs.getSystemPropertiesMap();
        assertNotNull(systemPropertiesMap);
        assertEquals(2, systemPropertiesMap.size());
        assertEquals("bar", systemPropertiesMap.get("foo"));
        assertEquals("lightning", systemPropertiesMap.get("beam.performance"));
    }

    @Test
    public void testArgsCloned() throws Exception {
        String[] args = {"Reproject", "ProjTarget.dim", "UnProjSource.dim"};
        CommandLineArgs lineArgs = CommandLineArgs.parseArgs(args);
        assertNotSame(args, lineArgs.getArgs());
        assertArrayEquals(args, lineArgs.getArgs());
    }

    @Test
    public void testNoArgsRequestsHelp() throws Exception {
        CommandLineArgs lineArgs = parseArgs();
        assertTrue(lineArgs.isHelpRequested());
    }

    @Test
    public void testHelpOption() throws Exception {
        CommandLineArgs lineArgs = parseArgs("Reproject", "-h");
        assertTrue(lineArgs.isHelpRequested());
        assertEquals("Reproject", lineArgs.getOperatorName());
        assertNull(lineArgs.getGraphFilePath());
        assertEquals(CommandLineArgs.DEFAULT_TARGET_FILEPATH, lineArgs.getTargetFilePath());
        assertEquals(CommandLineArgs.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> map = lineArgs.getSourceFilePathMap();
        assertNotNull(map);
        assertEquals(0, map.size());
    }

    @Test
    public void testOpOnly() throws Exception {
        CommandLineArgs lineArgs = parseArgs("Reproject");
        assertFalse(lineArgs.isHelpRequested());
        assertEquals("Reproject", lineArgs.getOperatorName());
        assertNull(lineArgs.getGraphFilePath());
        assertEquals(CommandLineArgs.DEFAULT_TARGET_FILEPATH, lineArgs.getTargetFilePath());
        assertEquals(CommandLineArgs.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> map = lineArgs.getSourceFilePathMap();
        assertNotNull(map);
        assertEquals(0, map.size());
    }

    @Test
    public void testOpWithSource() throws Exception {
        CommandLineArgs lineArgs = parseArgs("Reproject", "source.dim");
        assertEquals("Reproject", lineArgs.getOperatorName());
        assertNull(lineArgs.getGraphFilePath());
        assertEquals(CommandLineArgs.DEFAULT_TARGET_FILEPATH, lineArgs.getTargetFilePath());
        assertEquals(CommandLineArgs.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> sourceMap = lineArgs.getSourceFilePathMap();
        assertNotNull(sourceMap);
        assertEquals(3, sourceMap.size());
        assertEquals("source.dim", sourceMap.get("sourceProduct"));
        assertEquals("source.dim", sourceMap.get("sourceProduct.1"));
        assertEquals("source.dim", sourceMap.get("sourceProduct1")); // test for backward compatibility
    }

    @Test
    public void testOpWithTargetAndSource() throws Exception {
        CommandLineArgs lineArgs = parseArgs("Reproject", "-t", "output.dim", "source.dim");
        assertEquals("Reproject", lineArgs.getOperatorName());
        assertNull(lineArgs.getGraphFilePath());
        assertEquals("output.dim", lineArgs.getTargetFilePath());
        assertEquals(CommandLineArgs.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> sourceMap = lineArgs.getSourceFilePathMap();
        assertNotNull(sourceMap);
        assertEquals(3, sourceMap.size());
        assertEquals("source.dim", sourceMap.get("sourceProduct"));
        assertEquals("source.dim", sourceMap.get("sourceProduct.1"));
        assertEquals("source.dim", sourceMap.get("sourceProduct1")); // test for backward compatibility
    }

    @Test
    public void testMinimumGraph() throws Exception {
        CommandLineArgs lineArgs = parseArgs("./map-proj.xml", "source.dim");
        assertNull(lineArgs.getOperatorName());
        assertEquals("./map-proj.xml", lineArgs.getGraphFilePath());
        assertEquals(CommandLineArgs.DEFAULT_TARGET_FILEPATH, lineArgs.getTargetFilePath());
        assertEquals(CommandLineArgs.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> map = lineArgs.getSourceFilePathMap();
        assertNotNull(map);
        assertEquals("source.dim", map.get("sourceProduct"));
        assertEquals("source.dim", map.get("sourceProduct.1"));
        assertEquals("source.dim", map.get("sourceProduct1")); // test for backward compatibility
    }

    @Test
    public void testGraphOnly() throws Exception {
        CommandLineArgs lineArgs = parseArgs("./map-proj.xml");
        assertNull(lineArgs.getOperatorName());
        assertEquals("./map-proj.xml", lineArgs.getGraphFilePath());
        assertEquals(CommandLineArgs.DEFAULT_TARGET_FILEPATH, lineArgs.getTargetFilePath());
        assertEquals(CommandLineArgs.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        SortedMap<String, String> map = lineArgs.getSourceFilePathMap();
        assertNotNull(map);
    }

    @Test
    public void testFormatDetection() throws Exception {
        CommandLineArgs lineArgs = parseArgs("Reproject", "-t", "target.dim", "source.dim");
        assertEquals(CommandLineArgs.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
        String[] args = {"Reproject", "source.dim"};
        lineArgs = parseArgs(args);
        assertEquals(CommandLineArgs.DEFAULT_FORMAT_NAME, lineArgs.getTargetFormatName());
    }

    @Test
    public void testFormatOption() throws Exception {
        CommandLineArgs lineArgs;
        lineArgs = parseArgs("Reproject", "-t", "target.h5", "-f", "HDF-5", "source.dim");
        assertEquals("HDF-5", lineArgs.getTargetFormatName());
        lineArgs = parseArgs("Reproject", "-f", "GeoTIFF", "-t", "target.tif", "source.dim");
        assertEquals("GeoTIFF", lineArgs.getTargetFormatName());
        lineArgs = parseArgs("Reproject", "-f", "BEAM-DIMAP", "-t", "target.dim", "source.dim");
        assertEquals("BEAM-DIMAP", lineArgs.getTargetFormatName());
    }

    @Test
    public void testParameterOptions() throws Exception {
        CommandLineArgs lineArgs = parseArgs("Reproject", "-PpixelSizeX=0.02", "-PpixelSizeY=0.03", "-PpixelOffsetX=0.5", "-PpixelOffsetY=1.0", "source.dim");
        SortedMap<String, String> parameterMap = lineArgs.getParameterMap();
        assertEquals("0.02", parameterMap.get("pixelSizeX"));
        assertEquals("0.03", parameterMap.get("pixelSizeY"));
        assertEquals("0.5", parameterMap.get("pixelOffsetX"));
        assertEquals("1.0", parameterMap.get("pixelOffsetY"));
    }


    @Test
    public void testParametersFileOption() throws Exception {
        CommandLineArgs lineArgs = parseArgs("Reproject", "source.dim", "-p", "param.properties");
        assertEquals("param.properties", lineArgs.getParameterFilePath());
    }

    @Test
    public void testMetadataFileOption() throws Exception {
        CommandLineArgs lineArgs = parseArgs("Reproject");
        assertNull(lineArgs.getMetadataFilePath());
        lineArgs = parseArgs("Reproject", "-m", "metadata/reproject-md.properties");
        assertEquals("metadata/reproject-md.properties", lineArgs.getMetadataFilePath());
    }

    @Test
    public void testVelocityDirOption() throws Exception {
        CommandLineArgs lineArgs = parseArgs("Reproject");
        assertNull(lineArgs.getVelocityTemplateDirPath());
        lineArgs = parseArgs("Reproject", "-v", "metadata/vml");
        assertEquals("metadata/vml", lineArgs.getVelocityTemplateDirPath());
    }

    @Test
    public void testClearCacheAfterRowWrite() throws Exception {
        CommandLineArgs lineArgs;

        // test default value
        lineArgs = parseArgs("Reproject", "-x");
        assertTrue(lineArgs.isClearCacheAfterRowWrite());
    }

    @Test
    public void testJAIOptions() throws Exception {
        CommandLineArgs lineArgs;

        // test default value
        long oldCachValue = Config.instance().load().preferences().getLong("snap.jai.tileCacheSize", 512);
        try {
            Config.instance().load().preferences().putLong("snap.jai.tileCacheSize", 4024);
            lineArgs = parseArgs("Reproject", "source.dim");
            assertEquals(4219469824L, lineArgs.getTileCacheCapacity());
            assertEquals(getDefaultTileSchedulerParallelism(), lineArgs.getTileSchedulerParallelism());
        } finally {
            Config.instance().load().preferences().putLong("snap.jai.tileCacheSize", oldCachValue);
        }

        // test some valid value
        lineArgs = parseArgs("Reproject", "source.dim", "-c", "16M");
        assertEquals(16 * M, lineArgs.getTileCacheCapacity());
        assertEquals(getDefaultTileSchedulerParallelism(), lineArgs.getTileSchedulerParallelism());

        // test some valid value
        lineArgs = parseArgs("Reproject", "source.dim", "-q", "1", "-c", "16000K");
        assertEquals(16000 * K, lineArgs.getTileCacheCapacity());
        assertEquals(1, lineArgs.getTileSchedulerParallelism());

        // test some valid value
        lineArgs = parseArgs("Reproject", "source.dim", "-c", "16000002", "-q", "3");
        assertEquals(16000002, lineArgs.getTileCacheCapacity());
        assertEquals(3, lineArgs.getTileSchedulerParallelism());

        // test zero
        lineArgs = parseArgs("Reproject", "source.dim", "-c", "0", "-q", "10");
        assertEquals(0, lineArgs.getTileCacheCapacity());
        assertEquals(10, lineArgs.getTileSchedulerParallelism());

        // test zero or less
        try {
            parseArgs("Reproject", "source.dim", "-c", "-6");
            fail("Exception expected");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("negative"));
        }
    }

    @Test
    public void testSourceOptions() throws Exception {
        CommandLineArgs lineArgs = parseArgs("Reproject", "-SndviProduct=./inp/NDVI.dim", "-ScloudProduct=./inp/cloud-mask.dim", "-Pthreshold=5.0", "source.dim");
        SortedMap<String, String> sourceMap = lineArgs.getSourceFilePathMap();
        assertNotNull(sourceMap);
        assertEquals("./inp/NDVI.dim", sourceMap.get("ndviProduct"));
        assertEquals("./inp/cloud-mask.dim", sourceMap.get("cloudProduct"));
    }

    @Test
    public void testMultiSourceOptions() throws Exception {
        CommandLineArgs lineArgs = parseArgs("Reproject", "-Sndvi=./inp/NDVI.dim", "./inp/cloud-mask.dim", "source.dim", "input.dim");
        SortedMap<String, String> sourceMap = lineArgs.getSourceFilePathMap();
        assertNotNull(sourceMap);
        assertEquals("./inp/cloud-mask.dim", sourceMap.get("sourceProduct"));
        assertEquals("./inp/cloud-mask.dim", sourceMap.get("sourceProduct.1"));
        assertEquals("./inp/cloud-mask.dim", sourceMap.get("sourceProduct1")); // test for backward compatibility
        assertEquals("source.dim", sourceMap.get("sourceProduct.2"));
        assertEquals("source.dim", sourceMap.get("sourceProduct2")); // test for backward compatibility
        assertEquals("input.dim", sourceMap.get("sourceProduct.3"));
        assertEquals("input.dim", sourceMap.get("sourceProduct3")); // test for backward compatibility
        assertEquals("./inp/NDVI.dim", sourceMap.get("ndvi"));
    }

    @Test
    public void testUsageText() {
        String usageText = CommandLineUsage.getUsageText();
        assertNotNull(usageText);
        assertTrue(usageText.length() > 10);
    }

    @Test
    public void testUsageTextForOperator() {

        final String opName = "TestOpName";
        final String opDesc = "Creates a thing";
        final String srcProdAlias = "wasweissich";
        final String paramDefaultValue = "24.5";
        final String paramUnit = "Zwetschgen";
        final String paramDesc = "Wert Beschreibung";

        @OperatorMetadata(alias = opName, description = opDesc)
        class TestOp extends Operator {

            @Parameter(defaultValue = paramDefaultValue, unit = paramUnit, description = paramDesc)
            double value;

            @SourceProduct(alias = srcProdAlias)
            Product sourceProduct;

            @TargetProduct
            Product targetProduct;

            @Override
            public void initialize() throws OperatorException {
            }
        }

        class TestSpi extends OperatorSpi {

            public TestSpi() {
                super(TestOp.class);
            }
        }

        final TestSpi testSpi = new TestSpi();

        final GPF gpf = GPF.getDefaultInstance();
        final OperatorSpiRegistry spiRegistry = gpf.getOperatorSpiRegistry();
        spiRegistry.addOperatorSpi(testSpi);
        assertSame(testSpi, spiRegistry.getOperatorSpi(opName));
        String usageText = CommandLineUsage.getUsageTextForOperator(opName);

        assertEquals("Usage:\n" +
                     "  gpt TestOpName [options] \n" +
                     "\n" +
                     "Description:\n" +
                     "  Creates a thing\n" +
                     "\n" +
                     "\n" +
                     "Source Options:\n" +
                     "  -Swasweissich=<file>    Sets source 'wasweissich' to <filepath>.\n" +
                     "                          This is a mandatory source.\n" +
                     "\n" +
                     "Parameter Options:\n" +
                     "  -Pvalue=<double>    Wert Beschreibung\n" +
                     "                      Default value is '24.5'.\n" +
                     "                      Parameter unit is 'Zwetschgen'.\n" +
                     "\n" +
                     "Graph XML Format:\n" +
                     "  <graph id=\"someGraphId\">\n" +
                     "    <version>1.0</version>\n" +
                     "    <node id=\"someNodeId\">\n" +
                     "      <operator>TestOpName</operator>\n" +
                     "      <sources>\n" +
                     "        <wasweissich>${wasweissich}</wasweissich>\n" +
                     "      </sources>\n" +
                     "      <parameters>\n" +
                     "        <value>double</value>\n" +
                     "      </parameters>\n" +
                     "    </node>\n" +
                     "  </graph>\n",
                     usageText);
    }

    @Test
    public void testFailures() {
        testFailure(new String[]{"Reproject", "-p"}, "Option argument missing");
        testFailure(new String[]{"Reproject", "-f"}, "Option argument missing");
        testFailure(new String[]{"Reproject", "-t", "out.tammomat"}, "Output format unknown");
        testFailure(new String[]{"Reproject", "-ö"}, "Unknown option '-ö'");
        testFailure(new String[]{"Reproject", "-P=9"}, "Empty identifier");
        testFailure(new String[]{"Reproject", "-Pobelix10"}, "Missing '='");
        testFailure(new String[]{"Reproject", "-Tsubset=subset.dim",}, "Only valid with a given graph XML");
    }

    private void testFailure(String[] args, String reason) {
        try {
            parseArgs(args);
            fail("Exception expected for reason: " + reason);
        } catch (Exception e) {
            // ok
        }
    }

}

