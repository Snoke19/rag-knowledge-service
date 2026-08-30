# PDF Parser Limitations

The initial parser uses Apache PDFBox PDFTextStripper.

## Normal text

Result:
Normal text is extracted successfully and page order is preserved.

Decision:
Supported.

## Tables

Result:
Cell text is extracted, but the parser exposes only plain text.
Row/column semantics are not represented.

Decision:
Advanced table extraction is out of scope.

## Multiple columns

Result:
Text is extracted, but the parser does not provide a semantic guarantee
that visual reading order will always be preserved.

Decision:
Layout reconstruction is out of scope.

## Headers and footers

Result:
Headers and footers are extracted as ordinary text.

Decision:
No automatic header/footer removal is performed.

## Images

Result:
Text embedded in an image is not extracted by the text extractor.

Decision:
OCR is out of scope.

## Scanned PDFs

Result:
Image-only pages may contain little or no extracted text even when
the page is visually readable.

Decision:
OCR is out of scope.

## Encoding

Result:
[actual observed result from the fixture]

Decision:
[actual decision]