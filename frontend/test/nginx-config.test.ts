import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";

const configPath = new URL("../nginx.conf", import.meta.url);

test("routes health, catalog, loans, and SPA requests", () => {
  const config = readFileSync(configPath, "utf8");

  assert.match(config, /location = \/health/);
  assert.match(config, /location \/api\/books/);
  assert.match(config, /proxy_pass http:\/\/catalog-service:8080/);
  assert.match(config, /location \/api\/loans/);
  assert.match(config, /proxy_pass http:\/\/loan-service:8081/);
  assert.match(config, /try_files \$uri \$uri\/ \/index\.html/);
});
