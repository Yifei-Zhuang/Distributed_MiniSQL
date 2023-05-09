import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
// import React from 'react'
// import ReactMarkdown from 'react-markdown'
// import ReactDom from 'react-dom'
// import remarkGfm from 'remark-gfm'

// // const root = ReactDOM.createRoot(document.getElementById('root'));

// const markdown = `A paragraph with *emphasis* and **strong importance**.

// > A block quote with ~strikethrough~ and a URL: https://reactjs.org.

// * Lists
// * [ ] todo
// * [x] done

// A table:

// | a | b |
// | - | - |
// `

// ReactDom.render(
//   <ReactMarkdown children={markdown} remarkPlugins={[remarkGfm]} />,
//   document.body
// )