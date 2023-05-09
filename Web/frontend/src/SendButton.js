import React from 'react';
import { Button } from '@douyinfe/semi-ui';



export default class SendButton extends React.Component 
{
    render() 
    {
        return (
                <Button theme='solid' type='primary' style={{ marginRight: 8 }}
                    onClick={() => 
                        {
                            console.log('SEND');
                            this.props.sendmsg();
                        }}
                    disabled={!this.props.enable}
                        
                >SEND</Button>
        );
    }
}

