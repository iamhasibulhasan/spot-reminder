import React, { useEffect, useRef, useState } from 'react';
import { StyleProp, StyleSheet, View, ViewStyle } from 'react-native';
import { WebView, WebViewMessageEvent } from 'react-native-webview';
import { useTheme } from '../theme';
import { Coord } from '../types';

export type MapSpot = Coord & { id: string; name: string; sub?: string; radius?: number; visited?: boolean };
export type MapPath = { coords: [number, number][]; color?: string };

type Props = {
  spots?: MapSpot[];
  paths?: MapPath[];
  me?: Coord | null;
  pick?: Coord | null;
  /** change this value to re-centre the map on `me` / fit everything */
  fitKey?: string | number;
  onTap?: (c: Coord) => void;
  onSpotPress?: (id: string) => void;
  style?: StyleProp<ViewStyle>;
};

// OpenStreetMap tiles rendered with Leaflet — free, no API key.
const HTML = `<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>html,body,#map{height:100%;margin:0;background:#e5e7eb}
.dark .leaflet-tile-pane{filter:invert(1) hue-rotate(180deg) brightness(.95) contrast(.9)}
.pin{width:30px;height:30px;border-radius:50% 50% 50% 0;transform:rotate(-45deg);border:3px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,.4)}
.me{width:18px;height:18px;border-radius:50%;background:#2563eb;border:3px solid #fff;box-shadow:0 0 0 6px rgba(37,99,235,.25)}
.leaflet-control-attribution{font-size:9px}</style></head>
<body><div id="map"></div><script>
var map=L.map('map',{zoomControl:false}).setView([23.8,90.4],6);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(map);
var layer=L.layerGroup().addTo(map);
function send(o){window.ReactNativeWebView.postMessage(JSON.stringify(o))}
map.on('click',function(e){send({type:'tap',lat:e.latlng.lat,lng:e.latlng.lng})});
function icon(c,cls){return L.divIcon({className:'',html:'<div class="'+cls+'" style="background:'+c+'"></div>',iconSize:[30,30],iconAnchor:cls=='pin'?[8,28]:[9,9]})}
window.setData=function(d){
  document.body.className=d.dark?'dark':'';
  layer.clearLayers();var all=[];
  (d.paths||[]).forEach(function(p){if(p.coords.length){
    L.polyline(p.coords,{color:p.color||'#0D9488',weight:5,opacity:.9}).addTo(layer);
    L.circleMarker(p.coords[0],{radius:7,color:'#fff',weight:2,fillColor:'#16a34a',fillOpacity:1}).addTo(layer);
    var e=p.coords[p.coords.length-1];L.circleMarker(e,{radius:7,color:'#fff',weight:2,fillColor:'#dc2626',fillOpacity:1}).addTo(layer);
    all=all.concat(p.coords)}});
  (d.spots||[]).forEach(function(s){
    var m=L.marker([s.lat,s.lng],{icon:icon(s.visited?'#16a34a':'#F97316','pin')}).addTo(layer);
    m.bindTooltip(s.name+(s.sub?'<br><small>'+s.sub+'</small>':''));
    m.on('click',function(){send({type:'spot',id:s.id})});
    if(s.radius)L.circle([s.lat,s.lng],{radius:s.radius,color:'#0D9488',weight:1,fillOpacity:.08}).addTo(layer);
    all.push([s.lat,s.lng])});
  if(d.me){L.marker([d.me.lat,d.me.lng],{icon:icon('#2563eb','me')}).addTo(layer);all.push([d.me.lat,d.me.lng])}
  if(d.pick){L.marker([d.pick.lat,d.pick.lng],{icon:icon('#dc2626','pin')}).addTo(layer)}
  if(d.fit){
    if(d.pick)map.setView([d.pick.lat,d.pick.lng],Math.max(map.getZoom(),14));
    else if(all.length>1)map.fitBounds(all,{padding:[40,40],maxZoom:16});
    else if(all.length==1)map.setView(all[0],15)}
};
send({type:'ready'});
</script></body></html>`;

const WV = WebView as unknown as React.ComponentType<any>; // react-native-webview's class typings clash with React 19 types

export function OsmMap({ spots, paths, me, pick, fitKey, onTap, onSpotPress, style }: Props) {
  const ref = useRef<any>(null);
  const [ready, setReady] = useState(false);
  const { dark } = useTheme();
  const lastFit = useRef<string | number | undefined>(undefined);

  useEffect(() => {
    if (!ready) return;
    const fit = lastFit.current !== fitKey;
    lastFit.current = fitKey;
    const data = { spots: spots ?? [], paths: paths ?? [], me: me ?? null, pick: pick ?? null, dark, fit };
    ref.current?.injectJavaScript(`window.setData(${JSON.stringify(data)});true;`);
  }, [ready, spots, paths, me, pick, fitKey, dark]);

  const onMessage = (e: WebViewMessageEvent) => {
    const m = JSON.parse(e.nativeEvent.data);
    if (m.type === 'ready') setReady(true);
    else if (m.type === 'tap') onTap?.({ lat: m.lat, lng: m.lng });
    else if (m.type === 'spot') onSpotPress?.(m.id);
  };

  return (
    <View style={[styles.box, style]}>
      <WV
        ref={ref}
        originWhitelist={['*']}
        source={{ html: HTML, baseUrl: 'https://spot-reminder.app' }}
        onMessage={onMessage}
        javaScriptEnabled
        domStorageEnabled
        style={styles.web}
        nestedScrollEnabled
      />
    </View>
  );
}

const styles = StyleSheet.create({ box: { overflow: 'hidden', backgroundColor: '#e5e7eb' }, web: { flex: 1, backgroundColor: 'transparent' } });
