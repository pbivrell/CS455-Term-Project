package main

import (
    "net/http"
    "fmt"
    "io/ioutil"
    "strconv"
    "net"
    "bufio"
    "strings"
)

func main() {
    http.Handle("/", http.FileServer(http.Dir("web/")))
    http.HandleFunc("/email", handleEmail)
    http.HandleFunc("/locations", handleLocation)
    err := http.ListenAndServe(":45300", nil)
    if err != nil {
        panic(err)
    }
}

func handleEmail(rw http.ResponseWriter, req *http.Request) {
    body, _ := ioutil.ReadAll(req.Body)
    fmt.Println("Read Email")
    res := sendToKevinsCluster(string(body))
    fmt.Fprintf(rw,res)
}

func handleLocation(rw http.ResponseWriter, req *http.Request) {
    body, _ := ioutil.ReadAll(req.Body)
    fmt.Println("Read Location")
    res := sendToDanielsCluster(string(body))
    fmt.Fprintf(rw,res)
}

func sendToDanielsCluster(data string) string{
    cons := []string{"helena.cs.colostate.edu:47339","providence.cs.colostate.edu:47339","juneau.cs.colostate.edu:47339","concord.cs.colostate.edu:47339","charleston.cs.colostate.edu:47339","boise.cs.colostate.edu:47339","bismarck.cs.colostate.edu:47339","topeka.cs.colostate.edu:47339","dover.cs.colostate.edu:47339","carson-city.cs.colostate.edu:47339","sacramento.cs.colostate.edu:47339"}
    //fmt.Println(data)
    return connectAndSend(cons, data)
}

func sendToKevinsCluster(data string) string {
    cons := []string{"olympia.cs.colostate.edu:45409","boise.cs.colostate.edu:45409","albany.cs.colostate.edu:45409","annapolis.cs.colostate.edu:45409","baton-rouge.cs.colostate.edu:45409", "bismarck.cs.colostate.edu:45409","sacramento.cs.colostate.edu:45409", "carson-city.cs.colostate.edu:45409","charleston.cs.colostate.edu:45409","cheyenne.cs.colostate.edu:45409","austin.cs.colostate.edu:45409", "salem.cs.colostate.edu:45409","phoenix.cs.colostate.edu:45409", "pierre.cs.colostate.edu:45409","salt-lake-city.cs.colostate.edu:45409"}
    return connectAndSend(cons, data)
}

func connectAndSend(cons []string, data string) string{
    for _, machine := range(cons) {
        conn, err := net.Dial("tcp",machine)
        //fmt.Println("Attempting to connect to ",machine)
        if err == nil {
            fmt.Println("Connected to ", machine)
            lines := len(strings.Split(data,"\n"))
            fmt.Println("WROTE LINES: ", lines)
            fmt.Fprintf(conn, data)
            reader := bufio.NewReader(conn)
            s_size, _:= reader.ReadString('\n')
            s_size = strings.Replace(s_size,"\n","",-1)
            size, _:= strconv.Atoi(s_size)
            fmt.Println("READING LINES: ", size)
            res := ""
            for i := 0; i < size; i++ {
                data,_:= reader.ReadString('\n')
                res += data
            }
            fmt.Println("Done Reading")
            return res
        }
    }
    return "Error"
}

