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

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

import com.arangodb.ArangoDatabaseAsync;
import com.arangodb.ArangoDBAsync;

/**
 * Runtime transient data container for the ArangoDB Document Input Step
 * 
 * @author Adam Fowler {@literal <adam.fowler@hitachivantara.com>}
 * @since 1.0 2018-03-06
 */
public class ArangoDBDocumentInputData extends BaseStepData implements StepDataInterface {
  public RowMetaInterface outputRowMeta;
  public RowMetaInterface inputRowMeta;

  // in flight configuration objects here (E.g. batch handler
  public ArangoDBAsync arangoDB = null;
  public ArangoDatabaseAsync db = null;

  public int collectionFieldId = -1;
  public int docKeyFieldId = -1;
  public int contentFieldId = -1;

  /**
   * Default constructor
   */
  public ArangoDBDocumentInputData() {
    super();
  }

}