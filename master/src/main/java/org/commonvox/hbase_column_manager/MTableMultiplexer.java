/*
 * Copyright (C) 2016 Daniel Vimont
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.commonvox.hbase_column_manager;

import java.io.IOException;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.HTableMultiplexer;
import org.apache.hadoop.hbase.client.Put;

/**
 * Extension of standard HTableMultiplexer class which (in addition to providing all superclass
 * functionality) transparently performs ColumnManager repository processing, when appropriate.
 *
 * @author Daniel Vimont
 */
class MTableMultiplexer extends HTableMultiplexer {

  private final Repository repository;

  /**
   *
   * @param connection HBase Connection
   * @param perRegionServerBufferQueueSize determines the max number of the buffered Put ops for
   * each region server before dropping the request.
   * @throws IOException
   */
  MTableMultiplexer(Repository repository, Configuration conf, int perRegionServerBufferQueueSize)
          throws IOException {
    super(conf, perRegionServerBufferQueueSize);
    this.repository = repository;
  }

  /**
   * The put requests will be buffered by their corresponding buffer queues. Returns the list of
   * puts which could not be queued.
   *
   * @param tableName name of table to which puts are targeted.
   * @param puts list of puts to be queued.
   * @return list of puts which could not be queued.
   * @deprecated Use {@link #put(org.apache.hadoop.hbase.TableName, java.util.List)
   * put(TableName, List)} instead.
   */
  @Override
  @Deprecated
  public List<Put> put(byte[] tableName, List<Put> puts) {
    return this.put(TableName.valueOf(tableName), puts);
   }

  /**
   * The put request will be buffered by its corresponding buffer queue.
   *
   * @param tableName name of table to which put is targeted.
   * @param put put to be queued.
   * @return true if put was successfully queued.
   * @deprecated Use {@link #put(org.apache.hadoop.hbase.TableName, org.apache.hadoop.hbase.client.Put)
   * put(TableName, Put)} instead.
   */
  @Override
  @Deprecated
  public boolean put(byte[] tableName, Put put) {
    return this.put(TableName.valueOf(tableName), put);
  }

  /**
   * The put request will be buffered by its corresponding buffer queue.
   *
   * @param tableName name of table to which put is targeted.
   * @param put put to be queued.
   * @param retry number of times a put request is to be retried before being dropped.
   * @return true if put was successfully queued.
   * @deprecated Use {@link #put(org.apache.hadoop.hbase.TableName, org.apache.hadoop.hbase.client.Put, int)
   * put(TableName, Put, int)} instead.
   */
  @Override
  @Deprecated
  public boolean put(byte[] tableName, Put put, int retry) {
    return this.put(TableName.valueOf(tableName), put, retry);
  }

  /**
   * The put requests will be buffered by their corresponding buffer queues. Returns the list of
   * puts which could not be queued.
   *
   * @param tableName name of table to which puts are targeted.
   * @param puts list of puts to be queued.
   * @return list of puts which could not be queued.
   */
  @Override
  public List<Put> put(TableName tableName, List<Put> puts) {
    // NOTE: super method calls #put(tn, put, retry), so no ColumnManager processing here!
    return super.put(tableName, puts);
  }

  /**
   * The put request will be buffered by its corresponding buffer queue.
   *
   * @param tableName name of table to which put is targeted.
   * @param put put to be queued.
   * @return true if put was successfully queued.
   */
  @Override
  public boolean put(TableName tableName, Put put) {
    return put(tableName, put, 0, false);
  }

  /**
   * The put request will be buffered by its corresponding buffer queue.
   *
   * @param tableName name of table to which put is targeted.
   * @param put put to be queued.
   * @param retry number of times a put request is to be retried before being dropped.
   * @return true if put was successfully queued.
   */
  @Override
  public boolean put(TableName tableName, Put put, int retry) {
    return put(tableName, put, retry, true);
  }

  private boolean put(TableName tableName, Put put, int retry, boolean includeRetry) {
    boolean includedInRepositoryProcessing = false;
    MTableDescriptor mtd = null;
    if (repository.isActivated()
            && repository.isIncludedTable(tableName)) {
      includedInRepositoryProcessing = true;
      try {
        mtd = repository.getMTableDescriptor(tableName);
      // must catch IOException (HTableMultiplexer#put does NOT throw such an exception)
      } catch (IOException e) {
        repository.logIOExceptionAsError(e, this.getClass().getSimpleName());
        return false;
      }
    }
    // ColumnManager validation
    if (includedInRepositoryProcessing
            && mtd.hasColDescriptorWithColDefinitionsEnforced()) {
      try {
        repository.validateColumns(tableName, put);
      // must catch IOException (HTableMultiplexer#put does NOT throw such an exception)
      } catch (IOException e) {
        repository.logIOExceptionAsError(e, this.getClass().getSimpleName());
        return false;
      }
    }
    boolean putRequestQueued = false;
    // Alias processing
    if (includedInRepositoryProcessing
            && mtd.hasColDescriptorWithColAliasesEnabled()) {
      try {
        if (includeRetry) {
          putRequestQueued = super.put(
                  tableName, repository.convertQualifiersToAliases(mtd, put), retry);
        } else {
          putRequestQueued = super.put(
                  tableName, repository.convertQualifiersToAliases(mtd, put));
        }
      } catch (IOException e) {
        repository.logIOExceptionAsError(e, this.getClass().getSimpleName());
        return false;
      }
    } else {
    // Standard HBase processing
      if (includeRetry) {
        putRequestQueued = super.put(tableName, put, retry);
      } else {
        putRequestQueued = super.put(tableName, put);
      }
    }
    // ColumnManager auditing
    if (includedInRepositoryProcessing && putRequestQueued) {
      try {
        repository.putColumnAuditorSchemaEntities(tableName, put);
        // must catch IOException to enable override of HTableMultiplexer#put (which does NOT throw such an exception)
      } catch (IOException e) {
        repository.logIOExceptionAsError(e, this.getClass().getSimpleName());
        return false;
      }
    }
    return putRequestQueued;
  }
}
