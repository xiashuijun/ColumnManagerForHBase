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
/**
 * <hr><img src="doc-files/Tesseract_64pixels.jpg" alt="tesseract" height="56" width="56">
 * <b>ColumnManagerAPI for <a href="http://hbase.apache.org/" target="_blank">HBase™</a></b>
 * provides an extended <i>METADATA REPOSITORY SYSTEM for HBase</i>
 * with options for:<br><br>
 * <BLOCKQUOTE>
 * &nbsp;&nbsp;&nbsp;&nbsp;(1) <b>COLUMN AUDITING/DISCOVERY</b> -- captures Column metadata
 * (qualifier-name and max-length for each unique column-qualifier)
 * -- either via <a href="#column-auditing">real-time auditing</a>
 * as <i>Tables</i> are updated, or via a <a href="#discovery">discovery facility</a>
 * (direct-scan or mapreduce) for previously-existing <i>Tables</i>; the discovery process also
 * captures column-occurrences count and cell-occurrences count for each unique column-qualifier;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;(2) <b>COLUMN-ALIASING</b> --
 * involves a 4-byte (positive integer) column-alias being stored in each cell in place of the
 * full-length column-qualifier, potentially conserving considerable data
 * storage space; this works invisibly to the application developer, who continues working only
 * with the standard hbase-client API interfaces, reading and writing full-length
 * column-qualifiers;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;(3) <b>COLUMN-DEFINITION FACILITIES</b> --
 * administratively-managed <a href="ColumnDefinition.html">ColumnDefinitions</a>
 * (stipulating valid qualifier-name, column length, and/or value) may be created and
 * (a) optionally <a href="#enforcement">activated for column validation and enforcement</a>
 * as Tables are updated, and/or (b) used in the
 * <a href="#invalid-column-reporting">generation of various "Invalid Column" CSV-formatted reports</a>
 * (reporting on any column qualifiers, lengths, or values which do not adhere to
 * ColumnDefinitions);<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;(4) <b>SCHEMA EXPORT/IMPORT</b> -- provides
 * <a href="#export-import">schema (metadata) export and import facilities</a>
 * for HBase <i>Namespace</i>, <i>Table</i>, and all table-component structures;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;(5) <b>SCHEMA CHANGE MONITORING</b> -- tracks and provides an
 * <a href="#audit-trail">audit trail</a> for structural modifications made to
 * <i>Namespaces</i>, <i>Tables</i>, and <i>Column Families</i>.<br>
 * <br>
 *
 * <i>A basic</i> <b><a href="#command-line">COMMAND-LINE INTERFACE</a></b> <i>is also provided
 * for direct invocation of a number of the above-listed functions without any need for
 * Java coding.</i><br><br>
 *
 * Once it is installed and configured, standard usage of the ColumnManagerAPI in Java programs is
 * accomplished by simply substituting any reference to the standard HBase {@code ConnectionFactory}
 * class with a reference to the ColumnManager
 * <a href="MConnectionFactory.html">MConnectionFactory</a> class (as shown in the
 * <a href="#usage">USAGE IN APPLICATION DEVELOPMENT section</a> below).<br>
 * All other interactions with the HBase API are then to be coded as usual; ColumnManager will work
 * behind the scenes to capture and manipulate HBase metadata as stipulated by an
 * administrator/developer in <a href="#config">the ColumnManager configuration</a>.<br><br>
 * Any application coded with the ColumnManager API can be made to revert to standard HBase API
 * functionality simply by either (a) setting the value of the {@code column_manager.activated}
 * property to {@code <false>} in all {@code hbase-*.xml} configuration files, or (b) removing
 * that property from {@code hbase-*.xml} configuration files altogether.<br>
 * Thus, a ColumnManager-coded application can be used with ColumnManager <i>activated</i> in a
 * development and/or staging environment, but <i>deactivated</i> in production (where
 * ColumnManager's extra overhead might be undesirable).<br><br>
 * <b><a href="#toc">***Go to TABLE OF CONTENTS***</a></b><br><br>
 * </BLOCKQUOTE>
 * <i>HBase™ is a trademark of the <a href="http://www.apache.org/" target="_blank">
 * Apache Software Foundation</a>.</i><br><br>
 * <hr><b>FUTURE ENHANCEMENTS MAY INCLUDE:</b>
 * <ul>
 * <li><b>GUI interface:</b>
 * A JavaFX-based GUI interface could be built atop the ColumnManagerAPI, for administrative use on
 * Mac, Linux, and Windows desktops. Note that in the current release, a
 * <a href="#command-line">COMMAND-LINE INTERFACE</a>
 * is provided for some of the most crucial administrative functions.
 * </li>
 * </ul>
 * <hr>
 * This package transparently complements the standard HBase API provided by the Apache Software
 * Foundation in the packages <b>{@code org.apache.hadoop.hbase}</b> and
 * <b>{@code org.apache.hadoop.hbase.client}</b>.
 * <br>
 * <br>
 * <br>
 * <a name="toc"></a>
 * <hr style="height:3px;color:black;background-color:black">
 * <h3>Table of Contents</h3>
 * <ol type="I">
 * <li><a href="#prereq">PREREQUISITES</a></li>
 * <li><a href="#install">INSTALLATION</a></li>
 * <li><a href="#uninstall">UNINSTALLATION</a></li>
 * <li><a href="#config">BASIC CONFIGURATION (INCLUDE/EXCLUDE TABLES FOR ColumnManager PROCESSING)</a></li>
 * <li><a href="#usage">USAGE IN APPLICATION DEVELOPMENT</a></li>
 * <li><a href="#column-auditing">COLUMN AUDITING IN REAL-TIME</a></li>
 * <li><a href="#column-aliasing">COLUMN ALIASING</a></li>
 * <li><a href="#column-definition">COLUMN DEFINITION FACILITY</a></li>
 * <li><a href="#query">QUERYING THE REPOSITORY</a></li>
 * <li><a href="#admin">ADMINISTRATIVE TOOLS</a>
 * <li><a href="#command-line">COMMAND-LINE INVOCATION</a>
 * </ol>
 *
 * <a name="prereq"></a>
 * <hr style="height:3px;color:black;background-color:black">
 * <b>I. <u>PREREQUISITES</u></b>
 * <BLOCKQUOTE>
 * <b>HBase 1.x or later</b> -- HBase must be installed as per the installation instructions given in the
 * official
 * <a href="https://hbase.apache.org/book.html" target="_blank">Apache HBase Reference Guide</a>
 * (either in stand-alone, pseudo-distributed, or fully-distributed mode).
 * <br><br>
 * <b>HBase Java API</b> -- An IDE environment must be set up for HBase-oriented development using
 * the HBase Java API such that, at a minimum, an
 * <a href="https://gist.github.com/dvimont/a7791f61c4ba788fd827" target="_blank">HBase "Hello
 * World" application</a>
 * can be successfully compiled and run in it.
 * <br><br>
 * <b>JDK 7</b> -- HBase 1.x or later (upon which this package is dependent) requires JDK 7
 * or later.
 * </BLOCKQUOTE>
 * <a name="install"></a>
 * <hr style="height:3px;color:black;background-color:black">
 * <b>II. <u>INSTALLATION</u></b>
 * <BLOCKQUOTE>
 * <b>Step 1: Get the required JAR files, either (a) via direct download or
 * (b) by setting Maven project dependencies</b>
 * <br>
 * One of the most recently released versions of
 * <b><a href="https://github.com/dvimont/ColumnManagerForHBase/releases" target="_blank">
 * the JAR files for ColumnManager</a></b>
 * may be selected and  downloaded from GitHub and included in the IDE environment's compile
 * and run-time classpath configurations.
 * <br><br>
 * In the context of a <b>Maven project</b>, a dependency may be set in the project's
 * {@code pom.xml} file as in the following example. (Note that this example assumes a
 * current installation of HBase v1.2.1.):
 * <br><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;Remove the following <b>{@code hbase-client}</b> dependency element:
 * <pre>{@code      <dependency>
 *          <groupId>org.apache.hbase</groupId>
 *          <artifactId>hbase-client</artifactId>
 *          <version>1.2.1</version>
 *      </dependency>}</pre>
 * &nbsp;&nbsp;&nbsp;&nbsp;... and replace it with the following <b>{@code hbase-column-manager}</b>
 * dependency element:
 * <br>
 * <pre>{@code      <dependency>
 *          <groupId>org.commonvox</groupId>
 *          <artifactId>hbase-column-manager</artifactId>
 *          <version>1.2.1-beta-02</version>
 *      </dependency>}</pre>
 * <i>To access all currently-available versions of hbase-column-manager, please consult</i>
 * <b><a href="http://bit.ly/ColumnManagerMaven" target="_blank">
 * the Maven Central Repository</a></b>.
 * <br><br>
 * <a name="activate"></a>
 * <b>Step 2: Activate ColumnManager</b>
 * <br>
 * Add the following property element to either
 * <a href="https://hbase.apache.org/book.html#_configuration_files" target="_blank">the
 * &#60;hbase-site.xml&#62; file</a>
 * or an optional separate configuration file named <b>{@code <hbase-column-manager.xml>}</b>.
 * <pre>{@code      <property>
 *         <name>column_manager.activated</name>
 *         <value>true</value>
 *      </property>}</pre>
 * <i>NOTE</i> that the default for "{@code column_manager.activated}" is "{@code false}", so when
 * the property above is not present in {@code <hbase-site.xml>}
 * or in {@code <hbase-column-manager.xml>}, the ColumnManager API will
 * function exactly like the standard HBase API. Thus, a single body of code can operate <i>with</i>
 * ColumnManager functionality in one environment (typically, a development or testing
 * environment) and can completely <i>bypass</i>
 * ColumnManager functionality in another environment (potentially staging or production),
 * with the only difference between the environments being the presence or absence of the
 * "{@code column_manager.activated}" property in each environment's {@code hbase-*.xml}
 * configuration files.
 * <br>
 * <br>
 * <b>Step 3: Confirm installation (and create ColumnManager Repository <i>Namespace</i> and
 * <i>Table</i>)</b>
 * <br>
 * A successful invocation of the method <a href="MConnectionFactory.html#createConnection--">
 * MConnectionFactory.createConnection()</a>
 * will confirm proper installation of ColumnManager, as in the following code example:
 * <pre>{@code      import org.apache.hadoop.hbase.client.Connection;
 *      import org.commonvox.hbase_column_manager.MConnectionFactory;
 *
 *      public class ConfirmColumnManagerInstall {
 *          public static void main(String[] args) throws Exception {
 *              try (Connection connection = MConnectionFactory.createConnection()) {}
 *          }
 *      } }</pre> Note that the first invocation of <a href="MConnectionFactory.html#createConnection--">
 * MConnectionFactory.createConnection()</a> (as in the above code) will result in the automatic
 * creation of the ColumnManager Repository
 * <i>Namespace</i> ("{@code __column_manager_repository_namespace}") and <i>Table</i>
 * ("{@code column_manager_repository_table}").<br>
 * If the code above runs successfully, its log output will include a number of lines of Zookeeper
 * INFO output, as well as several lines of ColumnManager INFO output.
 * <br>
 * <br>
 * <b>Step 4: [OPTIONAL] Explicitly create Repository structures</b>
 * <br>
 * As an alternative to the automatic creation of the ColumnManager Repository
 * <i>Namespace</i> and <i>Table</i> in the preceding step, these structures may be explicitly
 * created through invocation of the static method
 * <a href="RepositoryAdmin.html#installRepositoryStructures-org.apache.hadoop.hbase.client.Admin-">
 * RepositoryAdmin#installRepositoryStructures</a>. Successful creation of these structures will
 * result in messages
 * such as the following appearing in the session's log output:
 * <pre>{@code      2015-10-09 11:03:30,184 INFO  [main] commonvox.hbase_column_manager: ColumnManager Repository Namespace has been created ...
 *      2015-10-09 11:03:31,498 INFO  [main] commonvox.hbase_column_manager: ColumnManager Repository Table has been created ...}</pre>
 * </BLOCKQUOTE>
 *
 * <a name="uninstall"></a>
 * <hr style="height:3px;color:black;background-color:black">
 * <b>III. <u>UNINSTALLATION</u></b>
 * <BLOCKQUOTE>
 * <b>Step 1: Deactivate ColumnManager</b>
 * <br>
 * Either remove the {@code column_manager.activated} property element from the environment's
 * {@code hbase-*.xml} configuration files or else set the property element's value to
 * {@code false}.
 * <br>
 * <br>
 * <b>Step 2: Invoke the {@code uninstall} method</b>
 * <br>
 * Invoke the static {@code RepositoryAdmin} method
 * <a href="RepositoryAdmin.html#uninstallRepositoryStructures-org.apache.hadoop.hbase.client.Admin-">
 * uninstallRepositoryStructures</a> to disable and delete the Repository table and to drop the
 * Repository namespace.
 * </BLOCKQUOTE>
 *
 * <a name="config"></a>
 * <hr style="height:3px;color:black;background-color:black">
 * <b>IV. <u>BASIC CONFIGURATION</u></b>
 * <BLOCKQUOTE>
 * <b>INCLUDE/EXCLUDE TABLES FOR ColumnManager PROCESSING</b>
 * <br>
 * <i><b>By default, when ColumnManager is installed and <a href="#activate">activated</a>,
 * all user Tables are included in ColumnManager processing.</b></i>
 * However, the following options are available
 * to limit ColumnManager processing to a specific subset of user Tables.
 * <BLOCKQUOTE>
 * <b>Option 1: Explicitly INCLUDE Tables for ColumnManager processing</b><br>
 * Specific <i>Tables</i> may optionally be explicitly
 * <b>included</b> in ColumnManager processing (with all others not specified being automatically
 * excluded).
 * This is done by adding the {@code [column_manager.includedTables]} property to either the
 * <a href="https://hbase.apache.org/book.html#_configuration_files" target="_blank">
 * &#60;hbase-site.xml&#62; file</a>
 * or in an optional, separate <b>{@code <hbase-column-manager.xml>}</b> file. Values are expressed
 * as fully-qualified <i>Table</i> names (for those <i>Tables</i> not in the default namespace,
 * the fully-qualified name is the <i>Namespace</i> name followed by the <i>Table</i> name,
 * delimited by a colon). Multiple values are delimited by commas, as in the following example:
 * <pre>{@code      <property>
 *         <name>column_manager.includedTables</name>
 *         <value>default:*,goodNamespace:myTable,betterNamespace:yetAnotherTable</value>
 *      </property>}</pre>
 * Note that all <i>Tables</i> in a given Namespace may be included by using an
 * asterisk {@code [*]} symbol in the place of a specific <i>Table</i> qualifier,
 * as in the example above which includes all Tables in the
 * "default" namespace via the specification, [{@code default:*}].<br><br>
 *
 * <b>Option 2: Explicitly EXCLUDE Tables from ColumnManager processing</b><br>
 *
 * Alternatively, specific <i>Tables</i> may optionally be explicitly
 * <b>excluded</b> from ColumnManager processing (with all others not specified being automatically
 * included).
 * This is done by adding the {@code [column_manager.excludedTables]} property to either the
 * <a href="https://hbase.apache.org/book.html#_configuration_files" target="_blank">
 * &#60;hbase-site.xml&#62; file</a>
 * or in an optional, separate <b>{@code <hbase-column-manager.xml>}</b> file. Values are expressed
 * as fully-qualified <i>Table</i> names (for those <i>Tables</i> not in the default namespace,
 * the fully-qualified name is the <i>Namespace</i> name followed by the <i>Table</i> name,
 * delimited by a colon). Multiple values are delimited by commas, as in the following example:
 * <pre>{@code      <property>
 *         <name>column_manager.excludedTables</name>
 *         <value>myNamespace:*,goodNamespace:myExcludedTable,betterNamespace:yetAnotherExcludedTable</value>
 *      </property>}</pre>
 * Note that all <i>Tables</i> in a given Namespace may be excluded by using an
 * asterisk {@code [*]} symbol in the place of a specific <i>Table</i> qualifier,
 * as in the example above which excludes all Tables in the
 * "myNamespace" namespace via the specification, [{@code myNamespace:*}].<br><br>
 * <i>Note also that if a {@code [column_manager.includedTables]} property is found in the
 * {@code <hbase-*.xml>}
 * files, then any {@code [column_manager.excludedTables]} property will be ignored.</i>
 * </BLOCKQUOTE>
 * </BLOCKQUOTE>
 *
 * <a name="usage"></a>
 * <hr style="height:3px;color:black;background-color:black">
 * <b>V. <u>USAGE IN APPLICATION DEVELOPMENT</u></b>
 * <br>
 * <BLOCKQUOTE>
 * <i>Two brief Gist examples are available on GitHub which illustrate basic usage of
 * ColumnManager:<br>(1)</i>
 * <a href="https://gist.github.com/dvimont/201081eef3316a1a1d4d83d571328010" target="_blank">
 * a Gist demonstrating usage of reporting/auditing functions</a>,
 * <i>and (2)</i>
 * <a href="https://gist.github.com/dvimont/66f0431ed6d5e6b86fe03653a298b958" target="_blank">
 * a Gist demonstrating usage and optional enforcement of ColumnDefinitions.</a>
 * </BLOCKQUOTE>
 * <BLOCKQUOTE>
 * <b>A. ALWAYS USE {@code MConnectionFactory} INSTEAD OF {@code ConnectionFactory}</b>
 * <BLOCKQUOTE>
 * To use ColumnManager in an HBase development environment, simply replace any reference to the
 * standard HBase API {@code ConnectionFactory} with a reference to ColumnManager's
 * <b><a href="MConnectionFactory.html">MConnectionFactory</a></b>
 * as follows:<br><br>
 * <u><i>Instead of</i></u><br>
 * <pre>{@code      import org.apache.hadoop.hbase.client.ConnectionFactory;
 *      ...
 *      Connection myConnection = ConnectionFactory.createConnection();
 *      ...}</pre>
 * <u><i>Use</i></u><br>
 * <pre>{@code      import org.commonvox.hbase_column_manager.MConnectionFactory;
 *      ...
 *      Connection myConnection = MConnectionFactory.createConnection();
 *      ...}</pre> Note that all Connection objects created in this manner generate special
 * {@code Admin}, {@code Table}, and {@code BufferedMutator} objects which (in addition to providing
 * all standard HBase API functionality) transparently interface with the ColumnManager Repository
 * for tracking and persisting of
 * <i>Namespace</i>, <i>Table</i>, <i>Column Family</i>, and
 * <i><a href="ColumnAuditor.html">ColumnAuditor</a></i> metadata. In addition,
 * ColumnManager-enabled {@code HTableMultiplexer} instances may be obtained via the method
 * <a href="RepositoryAdmin.html#createHTableMultiplexer-int-">RepositoryAdmin#createHTableMultiplexer</a>.
 * </BLOCKQUOTE>
 * <b>B. OPTIONALLY CATCH {@code ColumnManagerIOException} OCCURRENCES</b>
 * <BLOCKQUOTE>
 * In the context of some applications it may be necessary to perform special processing when a
 * <a href="ColumnManagerIOException.html">ColumnManagerIOException</a> is thrown, which may
 * signify rejection of a specific <i>Column</i> entry submitted in a {@code put}
 * (i.e., insert/update) to
 * an <a href="#enforcement">enforcement-enabled</a> <i>Table/Column-Family</i>.
 * In such cases, exceptions of this abstract
 * type (or its concrete subclasses) may be caught, and appropriate processing performed.
 * </BLOCKQUOTE>
 * </BLOCKQUOTE>
 *
 * <a name="column-auditing"></a>
 * <hr style="height:3px;color:black;background-color:black">
 * <b>VI. <u>COLUMN AUDITING IN REAL-TIME</u></b>
 * <BLOCKQUOTE>
 * When <a href="#activate">ColumnManager is activated</a> and <a href="#usage">usage has been
 * properly configured</a>,
 * <a href="ColumnAuditor.html">ColumnAuditor</a> metadata is gathered and persisted in the
 * Repository at runtime when Mutations (i.e. puts, appends, increments) are submitted via the
 * API to any <a href="#config">ColumnManager-included</a> <i>Table</i>.
 * All such metadata is then retrievable via the
 * <a href="RepositoryAdmin.html#getColumnAuditors-org.apache.hadoop.hbase.HTableDescriptor-org.apache.hadoop.hbase.HColumnDescriptor-">
 * RepositoryAdmin#getColumnAuditors</a> and
 * <a href="RepositoryAdmin.html#getColumnQualifiers-org.apache.hadoop.hbase.HTableDescriptor-org.apache.hadoop.hbase.HColumnDescriptor-">
 * RepositoryAdmin#getColumnQualifiers</a> methods.
 * <br><br>
 * Note that <a href="ColumnAuditor.html">ColumnAuditor</a> metadata may also be
 * gathered for previously-existing <i>Column</i>s via the
 * <a href="#discovery">RepositoryAdmin discovery methods</a>.
 * </BLOCKQUOTE>
 *
 * <a name="column-aliasing"></a>
 * <hr style="height:3px;color:black;background-color:black">
 * <b>VII. <u>COLUMN ALIASING</u></b>
 * <BLOCKQUOTE>
 * <hr>
 * Column-alias processing involves a 4-byte (positive integer) column-alias being stored in each
 * cell in place of the full-length <i>Column Qualifier</i>, potentially
 * conserving considerable data storage space; this works invisibly to the application developer,
 * who continues working only with the standard hbase-client API interfaces, reading and writing
 * full-length column-qualifiers.
 * <hr><br>
 * <b>Enable column aliasing:</b> The administrator may enable column-aliasing for a specified
 * <i>Column Family</i> via the
 * <a href="RepositoryAdmin.html#enableColumnAliases-boolean-org.apache.hadoop.hbase.TableName-byte:A-">
 * RepositoryAdmin#enableColumnAliases</a> method. Aliasing should only be activated for a
 * newly-defined, completely empty (or freshly truncated) <i>Column Family</i>, and it should not
 * be deactivated after data has been stored in the <i>Column Family</i>.
 *
 * </BLOCKQUOTE>
 *
 * <a name="column-definition"></a>
 * <hr style="height:3px;color:black;background-color:black">
 * <b>VIII. <u>COLUMN DEFINITION FACILITY</u>: manage <i>ColumnDefinition</i>s and enable enforcement</b>
 * <BLOCKQUOTE>
 * <hr>
 * A <a href="ColumnDefinition.html">Column Definition</a> pertains to a specific
 * <i>Column Qualifier</i> within a <i>Column Family</i> of a
 * <a href="#config">ColumnManager-included</a> <i>Table</i>, and permits optional stipulation of
 * <ul>
 * <li><u>Column Length</u>: valid maximum length of a value stored in HBase for the column,
 * and/or</li>
 * <li><u>Column Validation Regular Expression</u>: a regular expression that any value submitted
 * for storage in the column must match.</li>
 * </ul>
 * <hr><br>
 * <b>Manage ColumnDefinitions</b>: The <a href="ColumnDefinition.html">ColumnDefinitions</a>
 * of a <i>Column Family</i> are managed via a number of RepositoryAdmin
 * <a href="RepositoryAdmin.html#addColumnDefinition-org.apache.hadoop.hbase.TableName-byte:A-org.commonvox.hbase_column_manager.ColumnDefinition-">
 * add</a>,
 * <a href="RepositoryAdmin.html#getColumnDefinitions-org.apache.hadoop.hbase.HTableDescriptor-org.apache.hadoop.hbase.HColumnDescriptor-">
 * get</a>, and
 * <a href="RepositoryAdmin.html#deleteColumnDefinition-org.apache.hadoop.hbase.TableName-byte:A-byte:A-">
 * delete</a> methods.<br><br>
 * <b>Enable enforcement of ColumnDefinitions</b>: Enforcement of the
 * <a href="ColumnDefinition.html">ColumnDefinitions</a> of a given <i>Column Family</i> does
 * not occur until explicitly enabled via the method
 * <a href="RepositoryAdmin.html#enableColumnDefinitionEnforcement-boolean-org.apache.hadoop.hbase.TableName-byte:A-">
 * RepositoryAdmin#enableColumnDefinitionEnforcement</a>. This same method may be invoked to toggle
 * enforcement {@code off} again for the <i>Column Family</i>.<br><br>
 * When enforcement is enabled, then (a) any <i>Column Qualifier</i> submitted in a {@code put}
 * (i.e., insert/update) to the <i>Table:Column-Family</i> must correspond to an existing
 * {@code ColumnDefinition} of the <i>Column Family</i>, and (b) the corresponding <i>Column
 * value</i>
 * submitted must pass all validations (if any) stipulated by the {@code ColumnDefinition}. Any
 * {@code ColumnDefinition}-related enforcement-violation encountered during processing of a
 * {@code put} transaction will result in a
 * <a href="ColumnManagerIOException.html">ColumnManagerIOException</a>
 * (a subclass of the standard {@code IOException} class) being thrown: specifically, either a
 * <a href="ColumnDefinitionNotFoundException.html">ColumnDefinitionNotFoundException</a> or a
 * <a href="ColumnValueInvalidException.html">ColumnValueInvalidException</a>.
 * </BLOCKQUOTE>
 *
 * <a name="query"></a>
 * <hr style="height:3px;color:black;background-color:black">
 * <b>IX. <u>QUERYING THE ColumnManager REPOSITORY</u></b>
 * <ul>
 * <li>Get <i>Column Qualifier</i> names and additional column metadata:
 * <BLOCKQUOTE>
 * Subsequent to either the <a href="#column-auditing">capture of column metadata in real-time</a>
 * or its discovery via the <a href="#discovery">RepositoryAdmin discovery methods</a>,
 * a list of the <i>Column Qualifier</i>s belonging to a <i>Column Family</i>
 * of a <i>Table</i> may be obtained via the
 * <a href="RepositoryAdmin.html#getColumnQualifiers-org.apache.hadoop.hbase.HTableDescriptor-org.apache.hadoop.hbase.HColumnDescriptor-">
 * RepositoryAdmin#getColumnQualifiers</a> method.
 * Alternatively, a list of <a href="ColumnAuditor.html">ColumnAuditor</a> objects (containing
 * column qualifiers and additional column metadata) is obtained via the
 * <a href="RepositoryAdmin.html#getColumnAuditors-org.apache.hadoop.hbase.HTableDescriptor-org.apache.hadoop.hbase.HColumnDescriptor-">
 * RepositoryAdmin#getColumnAuditors</a> method.
 * </BLOCKQUOTE>
 * </li>
 * <li><a name="invalid-column-reporting"></a>Get Invalid Column reports which cite
 * discrepancies from <a href="ColumnDefinition.html">ColumnDefinitions</a>:<br>
 * <BLOCKQUOTE>
 * Subsequent to creation of <a href="ColumnDefinition.html">ColumnDefinitions</a> for a
 * <i>Table/ColumnFamily</i>, a CSV-formatted report listing columns which deviate from
 * those ColumnDefinitions (either in terms of qualifier-name, length, or value) may be
 * generated via the various
 * <a href="RepositoryAdmin.html#outputReportOnInvalidColumnQualifiers-java.io.File-org.apache.hadoop.hbase.TableName-boolean-boolean-">
 * RepositoryAdmin#outputReportOnInvalidColumn*</a> methods. If a method is run in
 * <i>verbose</i> mode, the outputted CSV file will include an entry (identified by the
 * fully-qualified column name and rowId) for each explicit invalid column that is found;
 * otherwise the report will contain a summary, giving a count of the invalidities associated
 * with a specific column-qualifier name. Note that invalid column report processing may optionally
 * be done via direct-scan or via mapreduce.
 * </BLOCKQUOTE>
 * </li>
 * <li><a name="audit-trail"></a>Get audit trail metadata:<br>
 * <BLOCKQUOTE>
 * A <a href="ChangeEventMonitor.html">ChangeEventMonitor</a> object (obtained via the method
 * <a href="RepositoryAdmin.html#getChangeEventMonitor--">RepositoryAdmin#getChangeEventMonitor</a>)
 * outputs lists of <a href="ChangeEvent.html">ChangeEvents</a>
 * (pertaining to structural changes made to user <i>Namespaces</i>, <i>Tables</i>,
 * <i>Column Families</i>, <i>ColumnAuditors</i>, and <i>ColumnDefinitions</i>) tracked by the
 * ColumnManager Repository.<br>
 * The ChangeEventMonitor's "get" methods allow for retrieving {@link ChangeEvent}s grouped and
 * ordered in various ways, and a static convenience method,
 * <a href="ChangeEventMonitor.html#exportChangeEventListToCsvFile-java.util.Collection-java.io.File-">
 * ChangeEventMonitor#exportChangeEventListToCsvFile</a>, is provided for outputting a list of
 * {@code ChangeEvent}s to a CSV file.
 * </BLOCKQUOTE>
 * </li>
 * </ul>
 *
 * <a name="admin"></a>
 * <hr style="height:3px;color:black;background-color:black">
 * <b>X. <u>ADMINISTRATIVE TOOLS</u></b>
 * <ul>
 * <li><a name="discovery"></a>HBase column-metadata discovery tools
 * <BLOCKQUOTE>
 * When ColumnManager is installed into an already-populated HBase environment, the
 * <a href="RepositoryAdmin.html#discoverColumnMetadata-boolean-">
 * RepositoryAdmin#discoverColumnMetadata</a> method
 * may be invoked to perform discovery of column-metadata
 * for all <a href="#config">ColumnManager-included</a> <i>Table</i>s.
 * Column metadata (for each unique column-qualifier value found) is persisted in the
 * ColumnManager Repository in the form of <a href="ColumnAuditor.html">ColumnAuditor</a> objects;
 * all such metadata is then retrievable via the
 * <a href="RepositoryAdmin.html#getColumnAuditors-org.apache.hadoop.hbase.HTableDescriptor-org.apache.hadoop.hbase.HColumnDescriptor-">
 * RepositoryAdmin#getColumnAuditors</a> and
 * <a href="RepositoryAdmin.html#getColumnQualifiers-org.apache.hadoop.hbase.HTableDescriptor-org.apache.hadoop.hbase.HColumnDescriptor-">
 * RepositoryAdmin#getColumnQualifiers</a> methods. Column discovery involves a full Table scan
 * (with KeyOnlyFilter), using either a direct-scan option or a mapreduce option.
 * </BLOCKQUOTE>
 * </li>
 * <li><a name="export-import"></a>HBase schema export/import tools
 * <BLOCKQUOTE>
 * The {@code RepositoryAdmin}
 * <a href="RepositoryAdmin.html#exportSchema-java.io.File-boolean-">
 * export methods</a> provide for creation of an external HBaseSchemaArchive (HSA) file (in XML
 * format*) containing the complete metadata contents (i.e., all <i>Namespace</i>, <i>Table</i>,
 * <i>Column Family</i>, <i>ColumnAuditor</i>, and  <i>ColumnDefinition</i> metadata) of either the
 * entire Repository or the user-specified <i>Namespace</i> or <i>Table</i>. Conversely, the
 * {@code RepositoryAdmin}
 * <a href="RepositoryAdmin.html#importSchema-java.io.File-boolean-">
 * import methods</a> provide for deserialization of a designated HSA file and importation of its
 * components into HBase (creating any Namespaces or Tables not already found in HBase).<br><br>
 * *An HSA file adheres to the XML Schema layout in
 * <a href="doc-files/HBaseSchemaArchive.xsd.xml" target="_blank">HBaseSchemaArchive.xsd.xml</a>.
 * <i>Note: Consistent with the HBase project's usage of XML, HBaseSchemaArchive XML documents
 * are not defined within a specific XML-namespace. In the context of XML processing in this
 * package, the requirement that a non-default XML-namespace be specified would seem to offer no
 * obvious benefit.</i>
 * </BLOCKQUOTE>
 * </li>
 * <li>Set "maxVersions" for ColumnManager Repository
 * <BLOCKQUOTE>
 * By default, the Audit Trail subsystem (as outlined in the subsection
 * <a href="#audit-trail">"Get audit trail metadata"</a> above) is configured to track and report on
 * only the most recent 50 {@code ChangeEvent}s of each entity-attribute that it tracks (for
 * example, the most recent 50 changes to the "durability" setting of a given
 * <i>Table</i>). This limitation relates directly to the default "maxVersions" setting of the
 * <i>Column Family</i> of the Repository <i>Table</i>. This setting may be changed through
 * invocation of the static method
 * <a href="RepositoryAdmin.html#setRepositoryMaxVersions-org.apache.hadoop.hbase.client.Admin-int-">
 * RepositoryAdmin#setRepositoryMaxVersions</a>.
 * </BLOCKQUOTE>
 * </li>
 * </ul>
 *
 * <a name="command-line"></a>
 * <hr style="height:3px;color:black;background-color:black">
 * <b>XI. <u>COMMAND-LINE INVOCATION</u></b>
 * <BLOCKQUOTE>
 * The UtilityRunner facility is provided for direct command-line invocation of a subset of
 * administrative functions. It allows invocation of these functions without the need to
 * perform <a href="#install">installation</a> or <a href="#config">configuration</a> of the
 * full package. The following administrative functions are available via UtilityRunner:
 * <ul>
 * <li><b>exportSchema</b>: invokes the
 * <a href="RepositoryAdmin.html#exportSchema-java.io.File-org.apache.hadoop.hbase.TableName-">
 * RepositoryAdmin#exportSchema</a> method for a specified <i>Table</i> or <i>Namespace</i>.</li>
 * <li><b>getChangeEvents</b>: invokes one of the
 * <a href="ChangeEventMonitor.html#getChangeEventsForTable-org.apache.hadoop.hbase.TableName-boolean-">
 * ChangeEventMonitor#getChangeEvents*</a> methods for a specified <i>Table</i> or <i>Namespace</i>.</li>
 * <li><b>getColumnQualifiers</b>: invokes
 * <a href="RepositoryAdmin.html#discoverColumnMetadata-org.apache.hadoop.hbase.TableName-boolean-">
 * Column Qualifier discovery</a> for the specified <i>Table</i> or <i>Namespace</i>, and then invokes the
 * <a href="RepositoryAdmin.html#outputReportOnColumnQualifiers-java.io.File-org.apache.hadoop.hbase.TableName-">
 * RepositoryAdmin#outputReportOnColumnQualifiers</a> method to output the results to the
 * specified file.</li>
 * <li><b>getColumnQualifiersViaMapReduce</b>: performs the same tasks as the
 * {@code getColumnQualifiers} function outlined above, but uses
 * <a href="RepositoryAdmin.html#discoverColumnMetadata-org.apache.hadoop.hbase.TableName-boolean-">
 * mapreduce to perform Column Qualifier discovery</a>.</li>
 * <li><b>importSchema</b>: invokes the
 * <a href="RepositoryAdmin.html#importSchema-java.io.File-org.apache.hadoop.hbase.TableName-boolean-">
 * RepositoryAdmin#importSchema</a> method for a specified <i>Table</i> or <i>Namespace</i>.</li>
 * <li><b>uninstallRepository</b>: invokes the
 * <a href="RepositoryAdmin.html#uninstallRepositoryStructures-org.apache.hadoop.hbase.client.Admin-">
 * RepositoryAdmin#uninstallRepositoryStructures</a> method to remove the Repository Table and
 * any subsidiary HBase artifacts which are generated in execution of any of the above
 * functions.</li>
 * </ul>
 *
 * <u>TO USE -- DOWNLOAD THE ColumnManager JAR AND INVOKE DESIRED FUNCTIONS</u>:
 * <ul>
 * <li>The {@code JAR} file corresponding to your currently-installed version of HBase
 * must be <a href="https://github.com/dvimont/ColumnManagerForHBase/releases" target="_blank">
 * downloaded from Github</a> or from <a href="http://bit.ly/ColumnManagerMaven" target="_blank">
 * the Maven Central Repository</a>. <i>(For example, {@code hbase-column-manager-1.0.3-beta-02.jar}
 * would be used with an HBase 1.0.3 installation.)</i>
 * </li>
 * <li>
 * Command-line invocation of UtilityRunner functions may then be performed from within the
 * directory containing the ColumnManager JAR file, as outlined in the following usage instructions,
 * which are outputted by the UtilityRunner's help function:
 * </li>
 * </ul>
 * <pre>      {@code ====================
 *       usage: java [-options] -cp <hbase-classpath-entries>
 *              org.commonvox.hbase_column_manager.UtilityRunner -u <arg> -t <arg>
 *              -f <arg> [-h]
 *
 *           *** Note that <hbase-classpath-entries> must include
 *           *** $HBASE_HOME/lib/*:$HBASE_HOME/conf, where $HBASE_HOME
 *           *** is the path to the local HBase installation.
 *
 *       Arguments for ColumnManagerAPI UtilityRunner:
 *       ====================
 *        -u,--utility <arg>   Utility to run. Valid <arg> values are as follows:
 *                             exportSchema, getChangeEventsForTable,
 *                             getColumnQualifiers,
 *                             getColumnQualifiersViaMapReduce, importSchema,
 *                             uninstallRepository
 *        -t,--table <arg>     Fully-qualified table name; or submit '*' in place
 *                             of table qualifier (e.g., 'myNamespace:*') to
 *                             process all tables in a given namespace.
 *        -f,--file <arg>      Source/target file.
 *        -h,--help            Display this help message.
 *       ====================
 *
 *       FOR EXAMPLE, the exportSchema function might be invoked as follows from
 *       within the directory containing the ColumnManager JAR file:
 *
 *           java -cp *:$HBASE_HOME/lib/*:$HBASE_HOME/conf
 *               org.commonvox.hbase_column_manager.UtilityRunner
 *               -u exportSchema -t myNamespace:myTable -f myOutputFile.xml }</pre>
 * </BLOCKQUOTE>
 */
package org.commonvox.hbase_column_manager;
