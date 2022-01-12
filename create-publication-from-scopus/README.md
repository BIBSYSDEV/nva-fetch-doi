# CREATE PUBLICATION FROM SCOPUS

## Building:
NB. It might be needed to run gradle xsd2java separately in order to get the no.scopus.generated package.

## Source:
* Based on https://bitbucket.org/unit-norge-team/sentralimport/src/develop/
* SCOPUS xsd schemas collected from https://schema.elsevier.com/dtds/document/abstracts/
  * We had to comment line 1690 in resources/schema.scopus.xocs-ani515/mathml3/mathml3-presentation.xsd (due to a double 
reference)