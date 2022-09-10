
open Expln_utils_jsonParse
let {exn,promiseMap,promiseFlatMap} = module(Expln_utils_common)

@val external fetch: (string,'a) => Js_promise.t<{..}> = "fetch"

type beErr = {
    code: int,
    msg: string
}

type beResp<'a> = {
    data: Js.Option.t<'a>,
    err: Js.Option.t<beErr>,
}

let parseBeResp: (string, jsonAny=>'a) => beResp<'a> = (respStr:string, dataMapper:jsonAny=>'a) => {
    let parsed = parseObj(respStr, d => {
        data: d->objOpt("data", dataMapper),
        err: d->objOpt("err", d=>{
            code: d->int("code"),
            msg: d->str("msg"),
        }),
    })
    switch parsed {
    | Ok(r) => 
        switch r.data {
        | Some(_) => r
        | None => 
            switch r.err {
            | Some(_) => r
            | _ => exn(`BE response doesn't contain neither 'data' nor 'err': ${respStr}`)
            }
        }
    | Error(msg) => 
        {
            Js_console.log2(`An error occured during parse of BE response: ${respStr}`, msg)
            exn(msg)
        }
    }
}

type beFunc<'req,'resp> = 'req => Js_promise.t<beResp<'resp>>

let createBeFunc: (string, jsonAny => 'resp) => beFunc<'req,'resp> = 
    (url, respMapper) => req => fetch(url, {
        "method": "POST",
        "headers": {
            "Content-Type": "application/json;charset=UTF-8"
        },
        "body": Js.Json.stringifyAny(req)
    }) 
    -> promiseFlatMap(res => res["text"](.))
    -> promiseMap(text => parseBeResp(text,respMapper))


let beRespIsData: beResp<'a> => bool = resp => resp.data->Belt.Option.isSome

let beRespData: beResp<'a> => 'a = resp =>
    switch resp.data {
    | Some(d) => d
    | None => exn("Cannot get data from error response.")
    }

let beRespIsErr: beResp<'a> => bool = resp => resp.err->Belt.Option.isSome

let beRespErrCode: beResp<'a> => int = resp =>
    switch resp.err {
    | Some({code}) => code
    | None => exn("Cannot get error code from non-error response.")
    }

let beRespErrMsg: beResp<'a> => string = resp =>
    switch resp.err {
    | Some({msg}) => msg
    | None => exn("Cannot get error msg from non-error response.")
    }