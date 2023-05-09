import React from 'react';
import { Card, Popover, Avatar } from '@douyinfe/semi-ui';
import ReactMarkdown from 'react-markdown'
import ReactDom from 'react-dom'
import remarkGfm from 'remark-gfm'

export default class ResultCard extends React.Component
{
    render()
    {
        return (
            <div style={{textAlign: 'center', justifyContent: 'center'}}>
                <br></br>
                <div 
                    className='result-card'
                style={{textAlign: 'center', justifyContent: 'center'}}>
                    <Card style={{ width: 800,maxHeight: 10000, textAlign: 'center', justifyContent: 'center' }} >
                        {this.props.str}
                    </Card>
                </div>
                <br />
            </div>
        );
    }
}


