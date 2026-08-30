# PDF Parser Limitations

The current parser uses Apache PDFBox `PDFTextStripper`.

## Normal text

Observed:
Normal text is extracted successfully and page order is preserved.

Decision:
Supported.

## Headers and footers

Observed:
Headers and footers are extracted as ordinary text.

Decision:
No automatic removal is performed.

## Tables

Observed:
Table cell text is extracted, but the `DocumentPage` result contains plain text only. Row/column structure is not
represented.

Decision:
Advanced table extraction is out of scope.

## Multiple columns

Observed:
Text from both columns is extracted, but the parser does not guarantee semantic human reading order for arbitrary
multi-column layouts.

Decision:
Advanced layout reconstruction is out of scope.

## Image-only / scanned pages

Observed:
Image-only pages produce blank or non-useful extracted text.

Decision:
OCR is out of scope.

## Text embedded in images

Observed:
Text rendered inside an image is not extracted by `PDFTextStripper`.

Decision:
OCR is out of scope.

## Text encoding

Observed:
[write the actual result of the encoding test]

Decision:
[write the actual decision]

## Out of scope

- OCR
- advanced table extraction
- complex layout reconstruction
- LLM-based cleanup
- semantic section detection
- summarization
- chunking