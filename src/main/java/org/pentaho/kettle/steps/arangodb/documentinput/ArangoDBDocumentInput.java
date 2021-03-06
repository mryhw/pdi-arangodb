/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2018 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.kettle.steps.arangodb.documentinput;

import java.util.concurrent.CompletableFuture;

import com.arangodb.ArangoCollectionAsync;

import org.pentaho.di.core.database.arangodb.ArangoDBDatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.core.row.RowDataUtil;

public class ArangoDBDocumentInput extends BaseStep implements StepInterface {
  private static Class<?> PKG = ArangoDBDocumentInputMeta.class; // for i18n purposes, needed by Translator2!!

  private ArangoDBDocumentInputMeta meta;
  private ArangoDBDocumentInputData data;

  //private boolean first = true;

  /**
   * Standard constructor
   */
  public ArangoDBDocumentInput(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
      Trans trans) {
    super(stepMeta, stepDataInterface, copyNr, transMeta, trans);

    meta = (ArangoDBDocumentInputMeta) getStepMeta().getStepMetaInterface();
    data = (ArangoDBDocumentInputData) stepDataInterface;
  }

  /**
   * Processes a single row in the PDI stream
   */
  public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
    meta = (ArangoDBDocumentInputMeta) smi;
    data = (ArangoDBDocumentInputData) sdi;

    Object[] r = getRow(); // get row, set busy!

    if (null == r) {
      logRowlevel("Processing last ArangoDB row");

      // no more input to be expected...
      // TODO tell async database connection to wait then quit it's connection
      //data.arangoDB.shutdown();

      logRowlevel("Processing last ArangoDB row completed");
      setOutputDone();
      return false;
    }

    if (first) {
      first = false;
      logRowlevel("Processing first ArangoDB row");
      logRowlevel("Collection: " + meta.getCollection());
      logRowlevel("Doc Key Field: " + meta.getDocumentKeyField());
      logRowlevel("Doc Content Field: " + meta.getDocumentContentField());

      data.inputRowMeta = getInputRowMeta();//.clone();
      //data.outputRowMeta = getInputRowMeta().clone();
      //data.outputRowMeta = new RowMeta();
      //Class c = data.inputRowMeta.getClass();
      //logDebug("input row meta class: " + c.getName());
      //c = data.outputRowMeta.getClass();
      //logDebug("output row meta clone class: " + c.getName());
      meta.getFields(getInputRowMeta(), getStepname(), null, null, this, repository, metaStore);
      data.outputRowMeta = getInputRowMeta().clone(); // modified by previous step (getFields), so MUST be called AFTER it

      // copy field definitions over from input to output (as per field analysis plugin)

      //data.outputRowMeta = new RowMeta();
      /*
      RowMetaInterface irm = getInputRowMeta();
      List<ValueMetaInterface> ml = irm.getValueMetaList();
      for (ValueMetaInterface vmi: ml) {
        logRowlevel("Value Meta: " + vmi.getName());
        data.outputRowMeta.addValueMeta(vmi);
      }
      */
      /*
      data.outputRowMeta.addValueMeta(new ValueMeta( "Collection", ValueMetaInterface.TYPE_STRING ));
      data.outputRowMeta.addValueMeta(new ValueMeta( "Content", ValueMetaInterface.TYPE_STRING ));
      data.outputRowMeta.addValueMeta(new ValueMeta( "MimeType", ValueMetaInterface.TYPE_STRING ));
      data.outputRowMeta.addValueMeta(new ValueMeta( "Format", ValueMetaInterface.TYPE_STRING ));
      data.outputRowMeta.addValueMeta(new ValueMeta( "Uri", ValueMetaInterface.TYPE_STRING ));
      data.outputRowMeta.addValueMeta(new ValueMeta( "OK", ValueMetaInterface.TYPE_STRING ));
      */

      // get IDs of fields we require
      String colField = meta.getCollection();
      if (null != colField) {
        data.collectionFieldId = data.inputRowMeta.indexOfValue(colField);
      }
      String docKeyField = meta.getDocumentKeyField();
      if (null != docKeyField) {
        data.docKeyFieldId = data.inputRowMeta.indexOfValue(docKeyField);
      }
      String docContentField = meta.getDocumentContentField();
      if (null != docContentField) {
        data.contentFieldId = data.inputRowMeta.indexOfValue(docContentField);
      }

      try {
        ArangoDBDatabaseMeta dbMeta = ((ArangoDBDatabaseMeta) meta.getDatabaseMeta().getDatabaseInterface());
        data.arangoDB = dbMeta.getConnection();
        data.db = data.arangoDB.db(dbMeta.getDatabaseName());
      } catch (Exception e) {
        logError(BaseMessages.getString(PKG, "ArangoDBDocumentInput.Log.CannotConnect"), e);
      }
      
      logRowlevel("Processing first ArangoDB query row prep completed");

    } // end if for first row (initialisation based on row data)

    // 3. putRow
    try {
      getJsonDocument(data, r, data.db.collection((String) r[data.collectionFieldId]),
          (String)r[data.docKeyFieldId], data.contentFieldId);

    } catch (Exception ex) {
      logError(BaseMessages.getString(PKG, "ArangoDBDocumentInput.Log.ExceptionPuttingRow"), ex);
    }
    // Execute request and then create a row for each document

    // Don't output incoming row - done for us in the first row invocation
    // putRow(data.outputRowMeta, r);

    if (checkFeedback(getLinesRead())) {
      if (log.isBasic()) {
        logBasic(BaseMessages.getString(PKG, "ArangoDBDocumentInput.Log.LineNumber") + getLinesRead());
      }
    }

    return true;
  }

  /**
   * Initialises the data for the step (meta data and runtime data)
   */
  public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
    meta = (ArangoDBDocumentInputMeta) smi;
    data = (ArangoDBDocumentInputData) sdi;

    if (super.init(smi, sdi)) {

      return true;
    }
    return false;
  }

  @Override
  public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
    //meta = (ArangoDBDocumentInputMeta) smi;
    //data = (ArangoDBDocumentInputData) sdi;

    //data.outputRowData = null;
    data.outputRowMeta = null;
    data.inputRowMeta = null;
    data.db = null;
    data.docKeyFieldId = -1;
    data.contentFieldId = -1;

    logRowlevel("dispose called on ArangoDBDocumentInput step");

    super.dispose(smi, sdi);
  }





  private void getJsonDocument( ArangoDBDocumentInputData data, Object[] row, ArangoCollectionAsync collection, String documentKey, int contentFieldId )  throws Exception {
    final CompletableFuture<String> f = collection.getDocument( documentKey, String.class ); // TODO replace this with a batch fetch method

    f.whenComplete( ( doc, ex ) -> {
      Object[] outputRowData = RowDataUtil.createResizedCopy( row, data.outputRowMeta.size() );
      outputRowData[ contentFieldId ] = doc; // convert/extract full JSON as String
      logRowlevel("Document Content: " + doc);
      logRowlevel("Document Ex: " + ex);
      // push row in to results object
      try {
        putRow( data.outputRowMeta, outputRowData );
      } catch (KettleStepException kse) {
        logError(BaseMessages.getString(PKG, "ArangoDBDocumentInput.Log.ExceptionPuttingRow"), kse);
      }
    } );
    f.get();
  }


}
