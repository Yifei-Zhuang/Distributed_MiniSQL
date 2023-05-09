import React from 'react';
import { TextArea } from '@douyinfe/semi-ui';


export default class QuestionBlank extends React.Component
{
    render()
    {
        return (
            <div>
                <br/><br/>
                <TextArea maxCount={10000} style={{maxWidth:"500px", minHeight:"300px"}} 
                    rows={15}
                showClear onChange={(str)=>
                        {
                            // console.log(str)
                            this.props.onChange(str)
                        }
                    }/>
                <br></br>
            </div>
        )
    }
}


