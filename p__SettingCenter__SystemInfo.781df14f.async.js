(window.webpackJsonp=window.webpackJsonp||[]).push([[26],{INsO:function(m,i,t){"use strict";var u=t("gL5p"),a=t("nKUr"),y=t.n(a),g=function(n){var s=n.height,c=s===void 0?"100%":s,d=n.width,h=d===void 0?"100%":d,r=n.language,O=r===void 0?"sql":r,j=n.theme,l=j===void 0?"vs":j,e=n.options,o=e===void 0?{selectOnLineNumbers:!0,renderSideBySide:!1,autoIndent:"None",readOnly:!0,automaticLayout:!0}:e,v=n.code;return Object(a.jsx)(a.Fragment,{children:Object(a.jsx)(u.b,{width:h,height:c,language:O,value:v,options:o,theme:"vs-dark"})})};i.a=g},kT1w:function(m,i,t){"use strict";t.r(i);var u=t("R9oj"),a=t("ECub"),y=t("+L6B"),g=t("2/Rp"),b=t("tJVT"),n=t("Znn+"),s=t("ZTPi"),c=t("q1tI"),d=t("tMyG"),h=t("INsO"),r=t("kZX9");function O(){return Object(r.c)("api/system/getRootLog")}var j=t("VA6q"),l=t("HOj0"),e=t("nKUr"),o=s.a.TabPane,v=function(C){var L=Object(c.useState)("Nothing."),x=Object(b.a)(L,2),S=x[0],A=x[1];Object(c.useEffect)(function(){f()},[]);var f=function(){var I=O();I.then(function(E){E.datas&&A(E.datas)})};return Object(e.jsx)(d.a,{title:!1,children:Object(e.jsxs)(s.a,{defaultActiveKey:"metrics",size:"small",tabPosition:"top",style:{border:"1px solid #f0f0f0",backgroundColor:"#fff"},tabBarExtraContent:Object(e.jsx)(g.a,{icon:Object(e.jsx)(j.a,{}),onClick:f}),children:[Object(e.jsx)(o,{tab:Object(e.jsx)("span",{children:"\xA0 Metrics \xA0"}),children:Object(e.jsx)(a.a,{image:a.a.PRESENTED_IMAGE_SIMPLE,description:Object(l.a)("global.stay.tuned")})},"metrics"),Object(e.jsx)(o,{tab:Object(e.jsx)("span",{children:"\xA0 Configuration \xA0"}),children:Object(e.jsx)(a.a,{image:a.a.PRESENTED_IMAGE_SIMPLE,description:Object(l.a)("global.stay.tuned")})},"configuration"),Object(e.jsx)(o,{tab:Object(e.jsx)("span",{children:"\xA0 Logs \xA0"}),children:Object(e.jsx)(h.a,{code:S,language:"java",height:"70vh"})},"logs"),Object(e.jsx)(o,{tab:Object(e.jsx)("span",{children:"\xA0 Log List \xA0"}),children:Object(e.jsx)(a.a,{image:a.a.PRESENTED_IMAGE_SIMPLE,description:Object(l.a)("global.stay.tuned")})},"logList")]})})},P=i.default=v}}]);
