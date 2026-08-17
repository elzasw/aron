// Post-processes the TypeSpec-emitted OpenAPI: removes the discriminator
// property re-declared in child schemas (TypeSpec requires the literal in every
// child; openapi-generator then generates a child enum that cannot override the
// parent's typed getter and the Java build breaks). The parent keeps the
// property and the discriminator mapping - the classic OpenAPI inheritance
// shape every generator handles (CAM applies the same fix to its emitted spec
// by hand; here it is part of the build).
//
// Usage: node scripts/strip-discriminator-redeclaration.mjs <openapi.yaml>
import fs from "node:fs";
import { parse, stringify } from "yaml";

const file = process.argv[2];
if (!file) {
  console.error("Usage: strip-discriminator-redeclaration.mjs <openapi.yaml>");
  process.exit(1);
}

const doc = parse(fs.readFileSync(file, "utf8"));
const schemas = doc.components?.schemas ?? {};
let stripped = 0;

for (const schema of Object.values(schemas)) {
  const discriminator = schema.discriminator;
  if (!discriminator?.propertyName || !discriminator.mapping) {
    continue;
  }
  for (const ref of Object.values(discriminator.mapping)) {
    const child = schemas[ref.split("/").pop()];
    if (!child?.properties?.[discriminator.propertyName]) {
      continue;
    }
    delete child.properties[discriminator.propertyName];
    if (Object.keys(child.properties).length === 0) {
      delete child.properties;
    }
    if (Array.isArray(child.required)) {
      child.required = child.required.filter((p) => p !== discriminator.propertyName);
      if (child.required.length === 0) {
        delete child.required;
      }
    }
    stripped++;
  }
}

fs.writeFileSync(file, stringify(doc, { lineWidth: 0 }));
console.log(`strip-discriminator-redeclaration: ${stripped} child re-declaration(s) removed from ${file}`);
