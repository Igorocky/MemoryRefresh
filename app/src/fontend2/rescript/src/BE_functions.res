open BE_utils
open Expln_utils_jsonParse

type getDataReq = { id: int }
type getDataResp = { data: array<string> }
let getData: beFunc<getDataReq, getDataResp> = createBeFunc("/be/getAllData", o => {
    data: o->arr("data", asStr)
})
