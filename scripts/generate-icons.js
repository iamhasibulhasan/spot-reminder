// Generates app icon PNGs from SVG. Run: node scripts/generate-icons.js (needs `npm i -D sharp`)
const sharp = require('sharp');
const path = require('path');
const out = (f) => path.join(__dirname, '..', 'assets', f);

const pin = (fill, ring, dot) => `
  <ellipse cx="512" cy="835" rx="150" ry="36" fill="${ring}" opacity="0.35"/>
  <ellipse cx="512" cy="835" rx="100" ry="22" fill="${ring}" opacity="0.5"/>
  <path d="M512 160C376 160 268 268 268 402c0 178 244 405 244 405s244-227 244-405C756 268 648 160 512 160z" fill="${fill}"/>
  <circle cx="512" cy="402" r="104" fill="${dot}"/>
  <path d="M512 330v76l52 32" stroke="${fill}" stroke-width="30" stroke-linecap="round" stroke-linejoin="round" fill="none"/>`;

const grad = `<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#14B8A6"/><stop offset="1" stop-color="#0369A1"/></linearGradient></defs>`;
const svg = (body) => Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">${body}</svg>`);

(async () => {
  await sharp(svg(`${grad}<rect width="1024" height="1024" fill="url(#g)"/>${pin('#FFFFFF', '#042F2E', '#F97316')}`)).png().toFile(out('icon.png'));
  await sharp(svg(`${grad}<rect width="1024" height="1024" fill="url(#g)"/>`)).png().toFile(out('android-icon-background.png'));
  await sharp(svg(`<g transform="translate(512 512) scale(0.72) translate(-512 -512)">${pin('#FFFFFF', '#042F2E', '#F97316')}</g>`)).png().toFile(out('android-icon-foreground.png'));
  await sharp(svg(`<g transform="translate(512 512) scale(0.72) translate(-512 -512)"><path d="M512 160C376 160 268 268 268 402c0 178 244 405 244 405s244-227 244-405C756 268 648 160 512 160zM512 506a104 104 0 1 1 0-208 104 104 0 0 1 0 208z" fill="#000" fill-rule="evenodd"/></g>`)).png().toFile(out('android-icon-monochrome.png'));
  await sharp(svg(`<g transform="translate(512 512) scale(0.6) translate(-512 -512)">${pin('#14B8A6', '#14B8A6', '#F97316')}</g>`)).png().toFile(out('splash-icon.png'));
  await sharp(svg(`${grad}<rect width="1024" height="1024" fill="url(#g)"/>${pin('#FFFFFF', '#042F2E', '#F97316')}`)).resize(96, 96).png().toFile(out('favicon.png'));
  console.log('icons generated');
})();
