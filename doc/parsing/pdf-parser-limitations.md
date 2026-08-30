# PDF Parser Limitations

The current implementation uses Apache PDFBox `PDFTextStripper`.

## Normal text

Result:
Normal text is extracted successfully and page order is preserved.

Decision:
Supported.

## Headers and footers

Result:
Headers and footers are extracted as ordinary text.

Decision:
No automatic removal is performed.

## Tables

Result:
Cell text is extracted, but the application receives plain text only. There is no row/column structure in
`DocumentPage`.

Decision:
Advanced table extraction is out of scope.

## Multiple columns

Result:
Text from both columns is extracted, but the parser does not provide a semantic guarantee that visual reading order will
always be preserved.

Decision:
Advanced layout reconstruction is out of scope.

## Image-only / scanned pages

Result:
Image-only pages produce no useful text because PDFBox text extraction does not perform OCR.

Decision:
OCR is out of scope.

## Text embedded in images

Result:
Text drawn inside an image is not extracted by the text extractor.

Decision:
OCR is out of scope.

## Text encoding

Result:
[record the actual result of the encoding test]

Decision:
[record the actual decision]

## Scope

The initial parser intentionally supports basic text extraction only. OCR, semantic layout reconstruction, table
extraction, LLM cleanup, section detection, summarization, and chunking are handled by later concerns if needed.