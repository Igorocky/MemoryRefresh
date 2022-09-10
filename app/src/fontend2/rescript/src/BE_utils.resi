open Expln_utils_jsonParse

type beResp<'a>

type beFunc<'req,'resp> = 'req => Js_promise.t<beResp<'resp>>

let createBeFunc: (string, jsonAny => 'resp) => beFunc<'req,'resp>

let beRespIsData: beResp<'a> => bool
let beRespData: beResp<'a> => 'a
let beRespIsErr: beResp<'a> => bool
let beRespErrCode: beResp<'a> => int
let beRespErrMsg: beResp<'a> => string
