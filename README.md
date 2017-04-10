## Java Extraction Toolkit

JET, the Java Extraction Toolkit, provides a set of tools for
constructing information extraction systems:  systems for building, from English text, databses which capture specified classes of entities, semantic relations, and events.  The tools include sentence and word segmentation, lexicon look-up, name recognition and classification, part-of-speech tagging, chunking, dependency parsing, transformational regularization, within-document coreference resolution.  Additional tools are provided to extract entities, relations, and events conforming to the ACE 2005 specification.

## License

All code is distributed under an Apache 2.0 license.

## Installation

Download and unpack the latest binary release to directory *D*.
(The directory names in path D should not have any blanks.)
Set environment variable JET_HOME to *D* and 
put the directory of executable scripts on PATH

#### For Macs and Linux systems running a C shell (csh, tcsh)

setenv JET_HOME *D*

set path = ( *D*/bin $path )

#### For Macs and Linux systems running a Bourne shell (sh, bash)

JET_HOME=*D*

export JET_HOME

PATH=*D*/bin:$PATH 

export PATH

#### For Windows command line

set JET_HOME=*D*

set PATH=*D*\win;%PATH%
