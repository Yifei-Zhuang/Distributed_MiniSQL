import logo from './logo.svg';
import './App.css';
import SendButton from './SendButton';
import React from 'react';
import QuestionBlank from './QuestionBlank';
import ResultCard from './ResultCard';
import { Card, Space, Button } from '@douyinfe/semi-ui';
import axios from 'axios';
import ResultTable from './ResultTable';

var testmarkdown = `
| heading | b  |  c |  d  |
| - | :- | -: | :-: |
| cell 1 | cell 2 | 3 | 4 | 
`;

window.url = "http://localhost:3003"

class App extends React.Component
{
    constructor()
    {
      super()
      this.state = 
      {
        sql : "",
        msg : "",
        ans : "",
        enable: true,
        isquit: false,
        uid : "",
        buffer : "",
        log : ""
      }
      this.getInit()
    }

    getInit()
    {
      var uid
      this.setState 
      (
        {
          uid: "connecting...",
          enable: false
        }
      )
      axios({
          url: window.url + "/getinit",
          method: 'post',
          data: 
          {}
      }).then((response) => {
          uid = response.data
          this.setState 
          (
            {
              uid: uid,
              enable: true
            }
          )
      }).catch((error) => {
          console.log(error);
          uid = "fail to connect"
          this.setState 
          (
            {
              uid: uid,
              enable: false
            }
          )
      });
      
      return uid
    }

    changeMsg(newMsg)
    {
      console.log("msg update")
      this.setState(
      {
        sql : newMsg
      })
    }

    getMsg() 
    {
      var msg 
      var log
      this.setState 
      (
        {
          enable: false
        }
      )
      axios({
          url: window.url + "/getmsg",
          method: 'post',
          data: 
          {
            'uid': this.state.uid,
            'msg': this.state.sql
          }
      }).then((response) => {
          console.log(response.data);
          msg = response.data["res"]
          log = response.data["log"]
          this.setState 
          (
            {
              msg: msg,
              log: log,
              enable: true
            }
          )
      }).catch((error) => {
          console.log(error);
      });
      
      return msg
    }

    getBuffer()
    {
      var buffer
      this.setState 
      (
        {
          enable: false
        }
      )
      axios({
          url: window.url + "/getbuffer",
          method: 'post',
          data: 
          {
            'uid': this.state.uid
          }
      }).then((response) => {
          buffer = response.data
          this.setState 
          (
            {
              buffer: buffer,
              enable: true
            }
          )
      }).catch((error) => {
          console.log(error);
      });
      
      return buffer
    }

    getQuit()
    {
      this.setState 
      (
        {
          enable: false
        }
      )
      axios({
          url: window.url + "/getquit",
          method: 'post',
          data: 
          {
            'uid': this.state.uid
          }
      }).then((response) => 
      {
        this.setState
        (
          {
            isquit: true,
            enable: false,
            log: response.data
          }
        )
      }).catch((error) => {
          console.log(error);
      });
    }

    render()
    {
        return (
            <div className="App"
            style={{textAlign: 'center', justifyContent: 'center'}}
            >
              <div>
                {this.state.uid}
              </div>
                <QuestionBlank
                  onChange={(str) => {this.changeMsg(str)}}
                ></QuestionBlank>
                <Space></Space>
                <div>
                  <SendButton sendmsg = {() => 
                    {
                      this.getMsg()
                    }}
                    enable={this.state.enable}  
                  />
                  <Button 
                    disabled={!this.state.enable}
                    onClick=
                    {
                      ()=>
                      {
                        this.getBuffer()
                      }
                    }
                    theme='solid' type='secondary' style={{ marginRight: 8 }}>BUFFER</Button>
                  <Button 
                    onClick=
                    {
                      ()=>
                      {
                        this.getBuffer()
                      }
                    }
                    disabled={!this.state.enable}
                    theme='solid' type='tertiary' style={{ marginRight: 8 }}>REFLESH</Button>
                  <Button 
                    onClick=
                    {
                      ()=>
                      {
                        this.getQuit()
                      }
                    }
                    disabled={!this.state.enable}
                    theme='solid' type='danger' style={{ marginRight: 8 }}>QUIT</Button>
                </div>
                <Space></Space>
                  <Card>
                    <div
                      style={{textAlign: 'center', justifyContent: 'center'}}
                    >
                      <ResultTable 
                        style={{textAlign: 'center', justifyContent: 'center'}}
                        markdown={this.state.msg}
                      ></ResultTable>
                    </div>
                  </Card>
                  <Card>
                    {this.state.log}
                  </Card>
                  <Card>
                    {this.state.buffer}
                  </Card>

                
                
            </div>
        );
    }
}

export default App;

// // // import React from 'react'
// import ReactMarkdown from 'react-markdown'
// import ReactDom from 'react-dom'
// import remarkGfm from 'remark-gfm'
// // import Page from './Page';

// // // const markdown = `| Column 1 | Column 2 | Column 3 |
// // // | -------- | -------- | -------- |
// // // | Cell 1   | Cell 2   | Cell 3   |
// // // | Cell 4   | Cell 5   | Cell 6   |`;

// // // function App() {
// // //   return (
// // //     <div>
// // //       <div>test</div>
// // //       <ReactMarkdown children={markdown} remarkPlugins={[remarkGfm]}/>
// // //       <div>test2</div>
// // //     </div>
// // //   );
// // // }

// // // export default App;

// // const Markdown = ({ content }) => {
// //   const renderers = {
// //     table: ({ children }) => <table>{children}</table>,
// //     tableRow: ({ children }) => <tr>{children}</tr>,
// //     tableCell: ({ children }) => <td>{children}</td>,
// //     tableHeaderCell: ({ children }) => <th>{children}</th>
// //   };

// //   return (
// //     <ReactMarkdown
// //       children={content}
// //       remarkPlugins={[remarkGfm]}
// //       renderers={renderers}
// //       escapeHtml={false}
// //     />
// //   );
// // };
// // const markdown = `A paragraph with *emphasis* and **strong importance**.

// // > A block quote with ~strikethrough~ and a URL: https://reactjs.org.

// // * Lists
// // * [ ] todo
// // * [x] done

// // A table:



// // ---------|---------
// // | name   |   id |
// // ---------|---------
// // | hzzz   |  999 |
// // | hzzz1  | 1999 |
// // | hzzz2  | 2999 |
// // `

// // export default class App extends React.Component
// // {
// //   render()
// //   {
// //     return(
// //       // ReactDom.render(
// //       //   <ReactMarkdown children={markdown} remarkPlugins={[remarkGfm]} />,
// //       //   document.body
// //       // )
// //       <div>
// //         <ReactMarkdown children={markdown} remarkPlugins={[remarkGfm]} />
// //         <Markdown content={markdown} />
// //         {/* <Page></Page> */}
// //       </div>
// //     ) 
// //   }
// // }

// import React from "react";
// import { useTable } from "react-table";

// function Table({ columns, data }) {
//   const {
//     getTableProps,
//     getTableBodyProps,
//     headerGroups,
//     footerGroups,
//     rows,
//     prepareRow
//   } = useTable({
//     columns,
//     data
//   });

//   // Render the UI for your table
//   return (
//     <table
//       {...getTableProps()}
//       border={1}
//       style={{ borderCollapse: "collapse", width: "100%" }}
//     >
//       <thead>
//         {headerGroups.map((group) => (
//           <tr {...group.getHeaderGroupProps()}>
//             {group.headers.map((column) => (
//               <th {...column.getHeaderProps()}>{column.render("Header")}</th>
//             ))}
//           </tr>
//         ))}
//       </thead>
//       <tbody {...getTableBodyProps()}>
//         {rows.map((row, i) => {
//           prepareRow(row);
//           return (
//             <tr {...row.getRowProps()}>
//               {row.cells.map((cell) => {
//                 return <td {...cell.getCellProps()}>{cell.render("Cell")}</td>;
//               })}
//             </tr>
//           );
//         })}
//       </tbody>
//       <tfoot>
//         {footerGroups.map((group) => (
//           <tr {...group.getFooterGroupProps()}>
//             {group.headers.map((column) => (
//               <td {...column.getFooterProps()}>{column.render("Footer")}</td>
//             ))}
//           </tr>
//         ))}
//       </tfoot>
//     </table>
//   );
// }

// function App() {
//   const columns = React.useMemo(
//     () => [
//       {
//         Header: "Heading 1",
//         Footer: "Footer 1",
//         columns: [
//           {
//             Header: "Sub Heading 1a",
//             accessor: "firstcolumn"
//           },
//           {
//             Header: "Sub Heading 1b",
//             accessor: "secondcolumn"
//           }
//         ]
//       },
//       {
//         Header: "Heading 2",
//         Footer: "Footer 2",
//         columns: [
//           {
//             accessor: "thirdcolumn"
//           }
//         ]
//       }
//     ],
//     []
//   );

//   const data = React.useMemo(
//     () => [
//       {
//         firstcolumn: "Row 1 Column 1",
//         secondcolumn: "Row 1 Column 2",
//         thirdcolumn: "Row 1 Column 3"
//       },
//       {
//         firstcolumn: "Row 2 Column 1",
//         secondcolumn: "Row 2 Column 2",
//         thirdcolumn: "Row 2 Column 3"
//       }
//     ],
//     []
//   );

//   return <Table columns={columns} data={data} />;
// }

// export default App;


// import gfm from "https://cdn.skypack.dev/remark-gfm@1.0.0";
// import ResultTable from './ResultTable';


// function App() {
//   const tableStruct = `
//   | heading | b  |  c |  d  |
//   | - | :- | -: | :-: |
//   | cell 1 | cell 2 | 3 | 4 | 
//   `;

//   return (
//     <div className="App">
//       <h3>React-Markdown - Using remark-gfm plugin</h3>

//       <table style={{ width: "100%", borderCollapse: "collapse" }}>
//         <thead>
//           <tr>
//             <th>Element</th>
//             <th>Markdown Syntax</th>
//             <th>Rendered by React-Markdown</th>
//           </tr>
//         </thead>
//         <tbody>
//           <tr>
//             <td>Strikethrough</td>
//             <td>~Strikethrough~</td>
//             <td>
//               <ReactMarkdown remarkPlugins={[gfm]}>
//                 ~Strikethrough~
//               </ReactMarkdown>
//             </td>
//           </tr>
//           <tr className={"left"}>
//             <td>todo list (unchecked)</td>
//             <td>* [ ] List item unchecked</td>
//             <td>
//               <ReactMarkdown remarkPlugins={[gfm]}>
//                 * [ ] List item unchecked
//               </ReactMarkdown>
//             </td>
//           </tr>
//           <tr className={"left"}>
//             <td>todo list (checked)</td>
//             <td>* [x] List item checked</td>
//             <td>
//               <ReactMarkdown remarkPlugins={[gfm]}>
//                 * [x] List item checked
//               </ReactMarkdown>
//             </td>
//           </tr>
//           <tr className={"left"}>
//             <td>Tables</td>
//             <td>
//               | heading | b | c | d |<br />
//               | - | :- | -: | :-: |<br />| cell 1 | cell 2 | 3 | 4 |
//             </td>
//             <td>
//               <ReactMarkdown
//                 remarkPlugins={[gfm]}
//                 children={tableStruct}
//               ></ReactMarkdown>
//             </td>
//           </tr>
//         </tbody>
//       </table>

//       <ReactMarkdown
//         remarkPlugins={[gfm]}
//         children={tableStruct}
//       ></ReactMarkdown>

//       <ResultTable
//         markdown={tableStruct}
//       ></ResultTable>
//     </div>
//   );
// }

// // const rootElement = document.getElementById("root");
// // ReactDOM.render(
// //     <App />,
// //   rootElement
// // );

// export default App;

