### tuktu.ml.processors.decisiontrees.DecisionTreeTrainProcessor
Trains a decision tree model.

  * **id** *(type: string)* `[Required]`

  * **result** *(type: string)* `[Required]`

  * **config** *(type: object)* `[Required]`

    * **model_name** *(type: string)* `[Required]`
    - Name of the model to be trained. If a model with that name is already available, that model will be used for additional training. Otherwise a new model with this name will be instantiated.

    * **destroy_on_eof** *(type: boolean)* `[Optional, default = true]`
    - Will this model be cleaned up once EOF is reached.

    * **wait_for_store** *(type: boolean)* `[Optional, default = false]`
    - Whether to wait for the model to be stored in the model repository. Setting this to true will ensure the model exists when proceeding to the next processor.

    * **data_field** *(type: string)* `[Required]`
    - The field the data resides in. Data must be of type Seq[Int].

    * **label_field** *(type: string)* `[Required]`
    - The field the label is in. Value must be an integer.

    * **max_nodes** *(type: int)* `[Required]`
    - The maximum number of leaf nodes in the tree.

