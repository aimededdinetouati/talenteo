const fs = require('fs');
const files = fs.readdirSync('.').filter(f => f.match(/^\d{2}-.*\.md$/)).sort();
console.log('Found files:', files);
let content = '# Talenteo Technical Documentation\n\n';
for (const file of files) {
  content += fs.readFileSync(file, 'utf8') + '\n\n<div class="page-break"></div>\n\n';
}
fs.writeFileSync('documentation.md', content);
console.log('Created documentation.md');
