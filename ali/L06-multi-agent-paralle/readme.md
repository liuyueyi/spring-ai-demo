# L06-multi-agent-paralle

这个示例主要演示了多智能体协作中的并行执行的场景，在这个场景中，各子Agen可以并行执行，最终将结果进行汇聚

工程中主要以项目策划这个场景进行了功能演示

| Agent名称 | 角色描述 | outputKey | 
| --- | --- | --- | 
| creative_agent | 创意专家，提供新颖的活动创意 | creative_ideas | 
| budget_agent | 财务专家，制定预算规划 | budget_plan | 
| execution_agent | 执行专家，规划具体落地步骤 | execution_steps | 

相关博文请参照： 

- [多智能体实战 | 基于 Spring AI Alibaba 实现方案策划多智能体](https://mp.weixin.qq.com/s/iKOAzyAZxm1u5ooxcmCNBA)
