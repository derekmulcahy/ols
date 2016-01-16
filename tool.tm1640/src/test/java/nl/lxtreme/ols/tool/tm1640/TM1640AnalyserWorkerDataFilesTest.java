/*
 * OpenBench LogicSniffer / SUMP project 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *
 * 
 * Copyright (C) 2010-2011 J.W. Janssen, www.lxtreme.nl
 */
package nl.lxtreme.ols.tool.tm1640;


import static org.junit.Assert.*;

import java.net.*;
import java.util.*;

import nl.lxtreme.ols.api.acquisition.*;
import nl.lxtreme.ols.api.data.annotation.AnnotationListener;
import nl.lxtreme.ols.api.tools.*;
import nl.lxtreme.ols.test.*;
import nl.lxtreme.ols.test.data.*;

import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.*;


/**
 * (Parameterized) tests cases for {@link TM1640AnalyserTask}.
 */
@RunWith( Parameterized.class )
public class TM1640AnalyserWorkerDataFilesTest
{
  // VARIABLES

  private final String resourceName;
  private final int lineClk;
  private final int lineDin;
  private final int expectedDatagramCount;

  protected int clkIdx;
  protected int dinIdx;

  // CONSTRUCTORS

  /**
   * Creates a new TM1640AnalyserWorkerDataFilesTest instance.
   */
  public TM1640AnalyserWorkerDataFilesTest( final String aResourceName, final int aLineClk, final int aLineDin,
      final int aExpectedDatagramCount )
  {
    this.resourceName = aResourceName;
    this.lineClk = aLineClk;
    this.lineDin = aLineDin;
    this.expectedDatagramCount = aExpectedDatagramCount;
  }

  // METHODS

  /**
   * @return a collection of test data.
   */
  @Parameters
  @SuppressWarnings( "boxing" )
  public static Collection<Object[]> getTestData()
  {
    return Arrays.asList( new Object[][] { //
        // { resource name, CLK, DIN, datagramCount }
            { "tm1640_eight_dot.ols", 6, 7, 100 },	// 0
            { "tm1640_isc.ols", 6, 7, 239 },		// 1
        } );
  }

  /**
   * @param aDataSet
   * @param aEventName
   * @return
   */
  private static void assertDataCount( final TM1640DataSet aDataSet, final int aExpectedDataCount )
  {
    int count = 0;
    for ( TM1640Data data : aDataSet.getData() )
    {
      if ( !data.isEvent() )
      {
        count++;
      }
    }
    assertEquals( "Not all data datagrams were seen?!", aExpectedDataCount, count );
  }

  /**
   * Test method for
   * {@link nl.lxtreme.ols.tool.tm1640.TM1640AnalyserTask#doInBackground()}.
   */
  @Test
  public void testAnalyzeDataFile() throws Exception
  {
    TM1640DataSet result = analyseDataFile( this.resourceName );
    assertDataCount( result, this.expectedDatagramCount );
  }

  /**
   * Analyses the data file identified by the given resource name.
   * 
   * @param aResourceName
   *          the name of the resource (= data file) to analyse, cannot be
   *          <code>null</code>.
   * @return the analysis results, never <code>null</code>.
   * @throws Exception
   *           in case of exceptions.
   */
  private TM1640DataSet analyseDataFile( final String aResourceName ) throws Exception
  {
    URL resource = ResourceUtils.getResource( getClass(), aResourceName );
    AcquisitionResult container = DataTestUtils.getCapturedData( resource );
    ToolContext toolContext = DataTestUtils.createToolContext( container );

    ToolProgressListener progressListener = Mockito.mock( ToolProgressListener.class );
    AnnotationListener annotationListener = Mockito.mock( AnnotationListener.class );

    TM1640AnalyserTask worker = new TM1640AnalyserTask( toolContext, progressListener, annotationListener );
    worker.setClkIndex( this.lineClk );
    worker.setDinIndex( this.lineDin );

    // Simulate we're running in a separate thread by directly calling the main
    // working routine...
    TM1640DataSet result = worker.call();
    assertNotNull( result );

    this.clkIdx = worker.getClkIdx();
    this.dinIdx = worker.getDinIdx();

    return result;
  }
}
