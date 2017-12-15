var name = "language";
function chgLang() {
  var value = $("#btnchangelanguage").data("language");
  $("#btnchangelanguage").data("language",(value=="en")?"zh":"en");
  $("#btnchangelanguage").html((value=="en")?"中文":"English");
  //$("#btnchangelanguage").data("language",(value=="en")?"en":"zh");
  //SetCookie(name, value);
  //Cookies.set(name, value,{ expires: 365 });
  localStorage.language=value;
  freshLanguage();
}
function getCookie(name){ //取cookies函数
  // return Cookies.get(name); // => 'value'
  return localStorage.language;
}

function freshLanguage(){
  // debugger;
  // var uulanguage = (navigator.language || navigator.browserLanguage).toLowerCase();
  // if (uulanguage.indexOf("en") > -1) {
  //   $("[data-localize]").localize("text", {  //**主要的代码** jquery.localize.js 底层实现切换逻辑
  //     pathPrefix: "lang",
  //     language: "en",localjs:true
  //   });
  // } else {
  //   $("[data-localize]").localize("text", {
  //     pathPrefix: "lang",
  //     language: "zh",localjs:true
  //   });
  // };
  // debugger;
   //根据cookie选择语言
  if (getCookie(name) != "") {
    if (getCookie(name) == "zh") {
      $("[data-localize]").localize("text", {
        pathPrefix: "lang",
        language: "zh",localjs:true
      });
    }
    if (getCookie(name) == "en") {
      $("[data-localize]").localize("text", {
        pathPrefix: "lang",
        language: "en",localjs:true
      });
    }
  }
}


$(function() {
  // var uulanguage = (navigator.language || navigator.browserLanguage).toLowerCase();
  // if (uulanguage.indexOf("zh") > -1) {
  //   $("[data-localize]").localize("text", {  //**主要的代码** jquery.localize.js 底层实现切换逻辑
  //     pathPrefix: "lang",
  //     language: "zh",localjs:true
  //   });
  // } else {
  //   $("[data-localize]").localize("text", {
  //     pathPrefix: "lang",
  //     language: "en",localjs:true
  //   });
  // };
   //根据cookie选择语言
  if (getCookie(name) == "") {
    localStorage.language="zh";
  }

    if (getCookie(name) == "zh") {
      $("[data-localize]").localize("text", {
        pathPrefix: "lang",
        language: "zh",localjs:true
      });
    }
    if (getCookie(name) == "en") {
      $("[data-localize]").localize("text", {
        pathPrefix: "lang",
        language: "en",localjs:true
      });
    }
});