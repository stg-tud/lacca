'use strict';
import * as $i_$002fjavascript$002esvg from "/javascript.svg";
import * as $j_internal$002d3ebfae0cba70adf981029a0da5b1e4b5ab5d02c6 from "./internal-3ebfae0cba70adf981029a0da5b1e4b5ab5d02c6.js";
function $p_Lchart_chart$package$__setCounter$1__sr_IntRef__Lorg_scalajs_dom_Element__I__V($thiz, counter$1, element$1, count) {
  var ev$1 = count;
  $j_internal$002d3ebfae0cba70adf981029a0da5b1e4b5ab5d02c6.$n(counter$1).sr_IntRef__f_elem = ev$1;
  element$1.innerHTML = ("count is " + $j_internal$002d3ebfae0cba70adf981029a0da5b1e4b5ab5d02c6.$n(counter$1).sr_IntRef__f_elem);
}
export { $p_Lchart_chart$package$__setCounter$1__sr_IntRef__Lorg_scalajs_dom_Element__I__V as $p_Lchart_chart$package$__setCounter$1__sr_IntRef__Lorg_scalajs_dom_Element__I__V };
/** @constructor */
function $c_Lchart_chart$package$() {
}
export { $c_Lchart_chart$package$ as $c_Lchart_chart$package$ };
$c_Lchart_chart$package$.prototype = new $j_internal$002d3ebfae0cba70adf981029a0da5b1e4b5ab5d02c6.$h_O();
$c_Lchart_chart$package$.prototype.constructor = $c_Lchart_chart$package$;
/** @constructor */
function $h_Lchart_chart$package$() {
}
export { $h_Lchart_chart$package$ as $h_Lchart_chart$package$ };
$h_Lchart_chart$package$.prototype = $c_Lchart_chart$package$.prototype;
$c_Lchart_chart$package$.prototype.LiveChart__V = (function() {
  document.querySelector("#app").innerHTML = (("\r\n    <div>\r\n      <a href=\"https://vitejs.dev\" target=\"_blank\">\r\n        <img src=\"/vite.svg\" class=\"logo\" alt=\"Vite logo\" />\r\n      </a>\r\n      <a href=\"https://developer.mozilla.org/en-US/docs/Web/JavaScript\" target=\"_blank\">\r\n        <img src=\"" + $j_internal$002d3ebfae0cba70adf981029a0da5b1e4b5ab5d02c6.$as_T($i_$002fjavascript$002esvg.default)) + "\" class=\"logo vanilla\" alt=\"JavaScript logo\" />\r\n      </a>\r\n      <h1>Test </h1>\r\n      <div class=\"card\">\r\n        <button id=\"counter\" type=\"button\"></button>\r\n      </div>\r\n      <p class=\"read-the-docs\">\r\n        Click on the Vite logo to learn more\r\n      </p>\r\n    </div>\r\n  ");
  $m_Lchart_chart$package$().setupCounter__Lorg_scalajs_dom_Element__V(document.getElementById("counter"));
});
$c_Lchart_chart$package$.prototype.setupCounter__Lorg_scalajs_dom_Element__V = (function(element) {
  var counter = new $j_internal$002d3ebfae0cba70adf981029a0da5b1e4b5ab5d02c6.$c_sr_IntRef(0);
  element.addEventListener("click", ((e) => {
    $p_Lchart_chart$package$__setCounter$1__sr_IntRef__Lorg_scalajs_dom_Element__I__V(this, counter, element, ((1 + counter.sr_IntRef__f_elem) | 0));
  }));
  $p_Lchart_chart$package$__setCounter$1__sr_IntRef__Lorg_scalajs_dom_Element__I__V(this, counter, element, 0);
});
var $d_Lchart_chart$package$ = new $j_internal$002d3ebfae0cba70adf981029a0da5b1e4b5ab5d02c6.$TypeData().initClass($c_Lchart_chart$package$, "chart.chart$package$", ({
  Lchart_chart$package$: 1
}));
export { $d_Lchart_chart$package$ as $d_Lchart_chart$package$ };
var $n_Lchart_chart$package$;
function $m_Lchart_chart$package$() {
  if ((!$n_Lchart_chart$package$)) {
    $n_Lchart_chart$package$ = new $c_Lchart_chart$package$();
  }
  return $n_Lchart_chart$package$;
}
export { $m_Lchart_chart$package$ as $m_Lchart_chart$package$ };
//# sourceMappingURL=chart.chart$package$.js.map
