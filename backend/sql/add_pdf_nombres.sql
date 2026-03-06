-- Agregar columnas para almacenar los nombres de archivo originales
ALTER TABLE solicitudes_contrato
ADD COLUMN nombre_pdf_editable VARCHAR(255),
ADD COLUMN nombre_pdf_enviado VARCHAR(255),
ADD COLUMN nombre_pdf_firmado VARCHAR(255),
ADD COLUMN nombre_pdf_final VARCHAR(255);

-- Actualizar registros existentes con nombres por defecto basados en las rutas existentes
UPDATE solicitudes_contrato
SET nombre_pdf_editable = 'editable.pdf'
WHERE ruta_pdf_editable IS NOT NULL AND nombre_pdf_editable IS NULL;

UPDATE solicitudes_contrato
SET nombre_pdf_enviado = 'enviado.pdf'
WHERE ruta_pdf_enviado IS NOT NULL AND nombre_pdf_enviado IS NULL;

UPDATE solicitudes_contrato
SET nombre_pdf_firmado = 'firmado.pdf'
WHERE ruta_pdf_firmado IS NOT NULL AND nombre_pdf_firmado IS NULL;

UPDATE solicitudes_contrato
SET nombre_pdf_final = 'final.pdf'
WHERE ruta_pdf_final IS NOT NULL AND nombre_pdf_final IS NULL;
