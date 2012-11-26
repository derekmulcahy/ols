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
 * Copyright (C) 2006-2010 Michael Poppitz, www.sump.org
 * Copyright (C) 2010-2012 J.W. Janssen, www.lxtreme.nl
 */
package nl.lxtreme.ols.client.action;


import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.filechooser.*;

import nl.lxtreme.ols.client.*;
import nl.lxtreme.ols.client.export.*;
import nl.lxtreme.ols.common.session.*;
import nl.lxtreme.ols.ioutil.*;
import nl.lxtreme.ols.util.swing.*;
import nl.lxtreme.ols.util.swing.component.*;


/**
 * Provides an action for exporting annotations to a HTML/CSV report.
 */
public class ExportAnnotationsAction extends AbstractAction implements IManagedAction
{
  // CONSTANTS

  private static final long serialVersionUID = 1L;

  private static final String ID = "ExportAnnotations";

  // VARIABLES

  // CONSTRUCTORS

  /**
   * Creates a new {@link ExportAnnotationsAction} instance.
   */
  public ExportAnnotationsAction()
  {
    // TODO Auto-generated constructor stub
  }

  // METHODS

  /**
   * {@inheritDoc}
   */
  @Override
  public void actionPerformed( final ActionEvent aEvent )
  {
    final Window owner = SwingComponentUtils.getOwningWindow( aEvent );

    final String[] extensions = ExportUtils.getExportExtensions();
    assert extensions != null;

    final String preferredExtension = ( extensions.length == 0 ) ? "" : extensions[0];

    final File exportFileName = SwingComponentUtils.showFileSaveDialog( owner, //
        new FileNameExtensionFilter( "Valid export format(s)", extensions ) );

    if ( exportFileName != null )
    {
      final File actualFile = FileExtensionUtils.setFileExtension( exportFileName, preferredExtension );

      try
      {
        exportTo( actualFile );
      }
      catch ( IOException exception )
      {
        // Make sure to handle IO-interrupted exceptions properly!
        if ( !IOUtil.handleInterruptedException( exception ) )
        {
          JErrorDialog.showDialog( owner, "Export annotations failed!", exception );
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getId()
  {
    return ID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateState()
  {
    setEnabled( hasAnnotationData() );
  }

  /**
   * @param aFile
   */
  private void exportTo( final File aFile ) throws IOException
  {
    // TODO Auto-generated method stub
  }

  /**
   * @return <code>true</code> if there are annotations to export,
   *         <code>false</code> otherwise.
   */
  private boolean hasAnnotationData()
  {
    final Session session = Client.getInstance().getSession();
    // Session can only be null in cases where the client is starting up or
    // shutting down...
    return ( session != null ) && session.hasData();
  }
}
