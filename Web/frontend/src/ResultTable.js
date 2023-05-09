import logo from './logo.svg';
import './App.css';
import SendButton from './SendButton';
import React from 'react';
import QuestionBlank from './QuestionBlank';
import ResultCard from './ResultCard';
import { Card, Space } from '@douyinfe/semi-ui';
import axios from 'axios';
import ReactMarkdown from 'react-markdown'
import ReactDom from 'react-dom'
import remarkGfm from 'remark-gfm'
import gfm from "https://cdn.skypack.dev/remark-gfm@1.0.0";


export default class ResultTable extends React.Component
{
    render()
    {
        return (
            <Space>
                <div
                    style={{textAlign: 'center', justifyContent: 'center'}}
                >
                    <ReactMarkdown
                        remarkPlugins={[gfm]}
                        children={this.props.markdown}
                        style = {this.props.style}
                    ></ReactMarkdown>
                </div>
            </Space>
        )
    }
}