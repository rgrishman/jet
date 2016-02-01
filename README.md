# Jet (Java Extraction Toolkit)

## About JET

JET, the Java Extraction Tool, provides a variety of components for language analysis, such as sentence segmentation, name tagging, time expression tagging and normalization, part-of-speech tagging, partial parsing, and coreference analysis.  These components can be arranged in pipelines for different applications, and can be used either for interactive analysis of individual sentences, or 'batch' analysis of complete documents. Simple tools are provided for annotating documents and displaying annotated documents.  A full set of procedures are also provided for performing information extraction of entities, relations, and events following the ACE [Automatic Content Extraction] specifications.

JET is a work in progress, and continues being regularly expanded and updated.

## License

Jet Copyright ©1999-2014 Ralph Grishman

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 . Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License. 

## Downloading Jet

The most recent Jet distribution (including binary, source code, models, and documentation): [Jet 1.8.1](http://cs.nyu.edu/grishman/jet/jet-150509.tar.gz)

If you find Jet useful for your work or incorporate any part of it into software you distribute, we earnestly request that you notify Prof. Ralph Grishman.

## Installing Jet

You need Java 1.5 or later in order to run Jet.  It runs under Linux, Apple Mac OS X, and Windows through terminal windows.

Download the latest Jet distribution tar file and extract all the files with tar -xzvf.

The expanded directory will include

- jet-all.jar, the Jet 'executable' (including all libraries)
- bin, a directory with simple scripts for invoking Jet in Linux and Mac OS
- win, a directory with simple scripts for invoking Jet in Windows
- docs, a directory of user documentation files
- props, a directory of configuration files
- data, a directory of data files used by Jet
- acedata, a directory of additional data files used by Jet for Ace information extraction
- example, a directory of files giving an example for running Jet for Ace information extraction
- runAceExample, a script to run this example
- NOTICE, a copyright / license notice

In addition, the directory will contain the following files and directories for those who wish to recompile or modify Jet

- src, a directory with the Jet source files
- test, a directory with source files of Jet unit tests
- parser-stub-src, stub sources for a statistical parser
- lib, a directory containing other jar files required by Jet
- build.xml, scripts for building Jet using ant

If you plan on using the Tratz dependency parser, you will also need to download [parseModel.gz](http://cs.nyu.edu/grishman/jet/parseModel.gz) 
and put it in the jet/data directory.

For the best name tagger coverage, download [AceOntoMeneModel.gz](http://cs.nyu.edu/grishman/jet/AceOntoMeneModel.gz), uncompress it, put it in the jet/acedata directory, and then run with the properties

    NameTags.ME.fileName = ../acedata/AceOntoMeneModel
    WordClusters.fileName = brownClusters10-2014.txt

To use Jet,

- for Linux or Mac OS, add the bin directory to your path, for Windows, add win to your path
- set the environment variable JET_HOME to point to the top directory into which the Jet files have been unpacked

The documentation for the current release (also included in the download) is [here](http://cs.nyu.edu/grishman/jet/guide/Jet.html).
