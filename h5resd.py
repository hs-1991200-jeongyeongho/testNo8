import tensorflow as tf
from tensorflow import keras
from tensorflow.keras.layers import Layer
from tensorflow.keras import initializers, regularizers, constraints, optimizers, layers, callbacks
from tensorflow.keras import backend as K
from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences

# HDF5 파일 경로 설정
model_path = 'Attention/SMOTE_CNN_multiple_FT_20240527_050231.h5'

print(1)

def dot_product(x, kernel):
    """
    Wrapper for dot product operation, in order to be compatible with both
    Theano and Tensorflow
    Args:
        x (): input
        kernel (): weights
    Returns:
    """
    if K.backend() == 'tensorflow':
        return K.squeeze(K.dot(x, K.expand_dims(kernel)), axis=-1)
    else:
        return K.dot(x, kernel)

class AttentionWithContext(Layer):
    """
    Attention operation, with a context/query vector, for temporal data.
    Supports Masking.
    Follows the work of Yang et al. [https://www.cs.cmu.edu/~diyiy/docs/naacl16.pdf]
    "Hierarchical Attention Networks for Document Classification"
    by using a context vector to assist the attention
    # Input shape
        3D tensor with shape: `(samples, steps, features)`.
    # Output shape
        2D tensor with shape: `(samples, features)`.
    How to use:
    Just put it on top of an RNN Layer (GRU/LSTM/SimpleRNN) with return_sequences=True.
    The dimensions are inferred based on the output shape of the RNN.
    Note: The layer has been tested with Keras 2.0.6
    Example:
        model.add(LSTM(64, return_sequences=True))
        model.add(AttentionWithContext())
        # next add a Dense layer (for classification/regression) or whatever...
    """

    def __init__(self,
                 W_regularizer=None, u_regularizer=None, b_regularizer=None,
                 W_constraint=None, u_constraint=None, b_constraint=None,
                 bias=True, **kwargs):

        self.supports_masking = True
        self.init = initializers.get('glorot_uniform')

        self.W_regularizer = regularizers.get(W_regularizer)
        self.u_regularizer = regularizers.get(u_regularizer)
        self.b_regularizer = regularizers.get(b_regularizer)

        self.W_constraint = constraints.get(W_constraint)
        self.u_constraint = constraints.get(u_constraint)
        self.b_constraint = constraints.get(b_constraint)

        self.bias = bias
        super(AttentionWithContext, self).__init__(**kwargs)

    #     def get_config(self):
    #         config = super().get_config()
    #         config.update({
    #             "arg1": self.arg1,
    #             "arg2": self.arg2,
    #         })
    #         return config

    def get_config(self):
        config = super().get_config().copy()
        config.update({
            'W_regularizer': self.W_regularizer,
            'u_regularizer': self.u_regularizer,
            'b_regularizer': self.b_regularizer,
            'W_constraint': self.W_constraint,
            'u_constraint': self.u_constraint,
            'b_constraint': self.b_constraint,
            'bias': self.bias,
        })
        return config

    def build(self, input_shape):
        assert len(input_shape) == 3

        self.W = self.add_weight(shape=(input_shape[-1], input_shape[-1],),
                                 initializer=self.init,
                                 name='{}_W'.format(self.name),
                                 regularizer=self.W_regularizer,
                                 constraint=self.W_constraint)
        if self.bias:
            self.b = self.add_weight(shape=(input_shape[-1],),
                                     initializer='zero',
                                     name='{}_b'.format(self.name),
                                     regularizer=self.b_regularizer,
                                     constraint=self.b_constraint)

        self.u = self.add_weight(shape=(input_shape[-1],),
                                 initializer=self.init,
                                 name='{}_u'.format(self.name),
                                 regularizer=self.u_regularizer,
                                 constraint=self.u_constraint)

        super(AttentionWithContext, self).build(input_shape)

    def compute_mask(self, input, input_mask=None):
        # do not pass the mask to the next layers
        return None

    def call(self, x, mask=None):
        uit = dot_product(x, self.W)

        if self.bias:
            uit += self.b

        uit = K.tanh(uit)
        ait = dot_product(uit, self.u)

        a = K.exp(ait)

        # apply mask after the exp. will be re-normalized next
        if mask is not None:
            # Cast the mask to floatX to avoid float64 upcasting in theano
            a *= K.cast(mask, K.floatx())

        # in some cases especially in the early stages of training the sum may be almost zero
        # and this results in NaN's. A workaround is to add a very small positive number ε to the sum.
        # a /= K.cast(K.sum(a, axis=1, keepdims=True), K.floatx())
        a /= K.cast(K.sum(a, axis=1, keepdims=True) + K.epsilon(), K.floatx())

        a = K.expand_dims(a)
        weighted_input = x * a
        return K.sum(weighted_input, axis=1)

    def compute_output_shape(self, input_shape):
        return input_shape[0], input_shape[-1]

# 모델 불러오기
custom_objects = {'AttentionWithContext': AttentionWithContext}

print(2)

# 모델 불러오기
model = keras.models.load_model(model_path, custom_objects=custom_objects)


print(3)

# 모델 요약 정보 출력
model.summary()

print(4)

# 모델 테스트
import numpy as np

input_shape = model.input_shape
print("모델의 입력 형식:", input_shape)

print(5)

batch_size = 1
example_input3 = np.random.randint(1, 10000, size=(batch_size, input_shape[1]))

print(6)

# 입력 데이터 모양에 맞게 예제 데이터 생성 (여기서는 임의의 데이터 사용)
texts = ["야 나 여기 오면서 진짜 진짜 귀여운 고양이 봤어. 우리집 주변에 약간 돌아다니다 보면은 계속 보이는 고양이들 있잖아. 그중에 진짜 까만 고양이가 있어. 그래가지고 걔 눈이 또 노란색이다? 밤에 가다가 걔를 만나면 진짜 뭔가 시선이 느껴져서 옆에 딱 보잖아? 그럼 딱 그~ 노란 두 눈만 이렇게 나에게 광선을 발사하고 있는 거야. 그래서 기분이 너무 좋은 거 알지? 오다가 그런 얘들 만나면 그래서 고양이가 너무 키우고 싶은 거야. 근데 요즘에 최근에 내 친구가 고양이를 키우기 시작했어. 근데 늙어. 걔가 원래 고양이를 맡았다고 했거든. 근데 갑자기 키운다는 거야. 걔를 그래서 야 너 무슨 일이야? 무슨 술수를 부려서 그런 고양이를 얻게 된거야? 했더니 친구네 사촌 오빠가 원래 고양이랑 강아지를 키웠는데 사고를 치셔서 결혼을 갑자기 하게된 거야. 그래가지고 결혼한다고 고양이를 맡겼는데 인제 알러지 때문에 인제 그 친 내 친구가 고양이를 키우게 된 거지. 그래서 내가 걔네집에 고양이를 보러가기로 했다. 신나서 고양이가 왔다고 해서. 근데 갑자기 걔한테 톡이 온 거야. 내가 그때 운전면허 학원에 있었는데 톡이 와가지고 야 고양이 없어졌어 이러는 거야. 그래가지고 뭐 진짜? 나한테 거짓말 치는 거 아니고? 그랬더니 진짜 나갔대. 고양이가 집을 근데 아침까지 본 거야. 아침에 아빠가 새벽 여섯 시에 출근을 하려고 딱 아침에 딱 그렇게 아이컨택을 하고 인제 출근 하려고 걔 이름을 시로거든. 시로야 아빠 갔다 올게. 하면서 인사를 하려고 했는데 없는 거야. 그래서 아빠는 걔가 집에 숨은 줄 알았대. 가족들이 아침 내내 안보였는데 집에 온 지 얼마 안 됐으니까 숨어있나보다 하고 안 찾았대. 근데 오후에 딱보니까 진짜 아무 데도 없는 거야. 얘가 시로가 집에 아무 데도 없어. 그래서 내친구가 난리가 난 거지. 아~ 시로 어떡하지 어떡하지 했 근데 일단 친구집에 가기로 했으니까 친구집에 갔다? 그래가지고 앉아가지고 고양이 탐정 이야기를 하고 있었어. 친구가 고양이 탐정이 그냥 의뢰하는 데만 20만 원이래. 그래가지고 그~ 의뢰하면 20만 원 찾으면 20만 원을 또 주는 거야. 근데 내 친구가 맡겼는데 얼마 안 돼서 없어졌는데 너무 미안하잖아. 그래가지고 어떡해 그래도 맡겨야지 하면서 김피탕을 막 먹고 있었는데 걔네 사촌오빠가 딱 오면서 뭘 안고 오는 거야. 뭐 털달린 게. 그래서 나는 이 한 여름에 무슨 털옷을 입고 난리야. 이랬는데 고양이였던 거야. 그래서 진짜 고양이 데리고 오면서 야 나 오면서 시로 찾았어 이랬어. 그래가지고 너무 놀라가지고 친구는 울고 너무 놀라가지고 나는 어떻게 찾았어요? 이러는데 혹시 몰라서 집 앞에 차를 이렇게 천천히 하면서 가고 있었는데 고양이가 그 차를 보고 나왔대. 야옹 하면서 그래가지고 알아보고 나왔는진 어쩐는진 모르겠는데. 그래서 이거 딱 맨손으로 싸움을 해가지고 딱 잡아 온 거야. 그래서 발도 꼬질꼬질 털도 꼬질꼬질한데 집에 와가지고 밥도 먹고 물도 먹고 간식도 먹고 그렇게 보냈어. 음 그래가지고 아~ 난 이렇게 고양이를 진짜 진짜 좋아하거든. 근데 요새 고양이 싫어하는 사람들도 많잖아. 너는 어때 너 고양이 좋아해? 나 진짜 완전 좋아하지. 진짜 어~ 사실 엄마 아빠의 그~ 반대만 아니었으면 나 진짜 고양이 키웠다. 엄마가 뭐 쪼끔 보수적인 분이셔. 그래서 막 여자애들은 털 달린 동물 키우면 건강에 해롭다. 막 이러시면서 조금 지양을 하시거든. 뭐 아부 아부지는 쪼끔 괜찮으신데. 근데 우리는 엄마의 뜻이 쪼끔 세. 그래서 오빠도 털 달린 동물을 별로 안좋아해. 그래가지고 키울 수 없는 상황인데. 내가 진짜 그~ 독립만 할 수 있다면 독립만 할 수 있다면 이제 독립해서 돈을 모은 다음에 그~ 키울거야. 고양이를 고양이. 특히 네가 아까 전에 처음 말했던 까맣고 눈이 정말 예쁜 초롱초롱한 아이를 키울거야. 그러면 너는 만약에 동물 키우고 싶다면 어떤 동물 키우고 싶어? 키우고 싶은 동물이 나는 근데 사실 되게 많아. 되게 많아서 내가 책임을 못 질 걸 아니까 안 키우는 거거든. 나 유튜브로 자주 보는 게 있다면 고슴도치. 그리고 햄스터 친구들도 귀엽고 기니피그 친구들도 귀엽고 아~ 너무 귀엽잖아. 그~ 조끄마한 얘들이 막 뽈 뽈 뽈 뽈 뽈 기어 다니면서 그~ 씨앗 달라고 막 이렇게 하는 거 보면은. 그래서 근데 내가 생각해봤을 때 그런 얘들을 내가 키우면 제대로 못 할 거 같고 보는 거에 대한 귀여움 이렇게 친구 집 가서 보는 정도. 그리고 나 또 키워보고 싶은 게 있으면 뱀. 뱀이 생각보다 엄청 귀엽더라고. 하얀 뱀이 코 끝이 빨개지면은 흥분을 한 거래. 어~ 그거보고 너무 너무 귀엽잖아. 뱀 혓바닥도 날름날름하면서. 아~ 근데 뱀은 또 싫어하는 사람이 많으니까 이렇게 영상으로만 보고. 언제 만질 기회가 있음 만져보자 이런 생각? 내가 책임질 수 있는 그런 게 아니니까. 유튜브로 내가 자주 본다고 했잖아. 그럼 너 고양이 좋아하면 유튜브로도 고양이 같은 애들 많이 봐? 사실 유튜브로는 조금 많이 안봐. 딱 좋아하는 취향 유튜브가 하나 생기니까 잘 안보게 되더라고. 내가 보는 유튜브가 그~ 꼬북이 라는 꼬북이의 다이어리라는 그~ 유튜버이신데. 거기 있는 고양이들이 아~ 한 마리는 안타깝게 무지개 다리를 건넜는데 그 한 마리가 정말 예뻤어. 하얀색에 오드아이인 고양인데 어~ 정말 주인분들을 잘 따르고 애교도 막 부리고 막 야옹야옹 하면서 관심 달라고 그렇게 얘기를 하는 거야. 그 모습들이 너무 귀여운 거야. 게다가 그~ 동생 고양이가 있는데 동생 고양이는 어찌 장모종인데 걔가 캣타워에 딱 앉아서 꼬리를 이렇게 내린 모습이 정말 우아한 거야. 아~ 진짜 저~ 게다가 그 두 형제거든? 이제 남자애 남자애여가지고 형제란 말이야. 형제들이 진짜 와 정말 친하게 지내는 거야. 원래 막 고양이 다른 에스엔에스에서 막 고양이 두 마리씩 키우면 박터지게 싸우고 막 앙앙 물기도 하고 막 그러는데 걔네들은 진짜 싸우는 거 없이 막 얌전하게 그 그루밍도 해주고 너무 이쁜 거야. 아~ 그래서 계속 봤어. 아~ 언제는 날 잡고 그~ 그~ 꼬북이 고양이만 나오는 영상만 주루룩 봤을 정도라니까. 어~ 너무 귀여워. 심지어 이제 주인 앞에서 막 몸을 부풀린다고 몸을 부풀려서 막 앞에 쫑쫑쫑 가는 영상도 있었는데 너무 사랑스러울 수가 없는 거야. 정말이지 그렇게 해서 걔 그~ 유튜브만 계속 보게 되더라고. 정말 예뻤어. 그러면 너는 유튜브 채널은 안 봐? 그~ 요즘에 유튜브가 많이 유행하다 보니까 유튜브로 이제 도 애완동물 많이 보여주더라고. 아까 전에 뱀 키운다고 뱀을 키우고 싶다고 말했었잖아. 그러면 뱀 이제 키워는 유튜버도 있어 혹시? 뱀 키우는 유튜버. 내가 유튜브를 구독을 해놓고 잘 안 봐. 그래서 가끔 뭐 보고 싶을 때 아니면 피드에 딱 떴을 때 이렇게 타고 가거든. 근데 뱀만 키우는 거 그래서 유튜버들 이름이 생각이 잘 안나. 근데 막 파충류 같은 거 키우는 사람 있는 거 막 다흑 이런 사람. 근데 막 나 곤충 영상 영상도 많이 봐가지고 애완동물은 아니지만 그 사람들은 그걸 애완동물처럼 키우니까. 막 파브르 이런 사람들 많이 보고. 그리고 유튜브 영상하면 요즘에는 강아지 얘들이 엄청 많이 나오잖아. 강아지 중에 내가 보는 게 소녀의 행성이라고. 소녀랑 아~ 행성이랑 우주 맞나? 소녀 행성 우주 해가지고 리트리버 포메라니안 그리고 코기. 이 셋이 하는데 진짜 집은 난장판인데 너무너무 귀엽더라고. 그래서 내 약간 우리의 로망 같은 거 있잖아. 그~ 집이 또 이사를 가가지고 앞에 마당이 있잖아. 마당이 있는 게 너무 예뻐 보이더라고. 우리 지금까지 고양이 얘기만 했는데 혹시 너 강아지도 좋아해? 꼭 고양이 좋아하는 사람 있고 강아지만 좋아하는 사람 있고 엄청 많더라고. 사람들이 강아지도 정말 좋아하지. 아까 전에 네가 소녀의 행성 이제 얘기를 했었잖아. 나도 그~ 유튜브 채널을 봤단 말이야. 주로 어떤 걸 보냐면 그~ 리트리버랑 그~ 친구들이 그~ 에이에스엠알하는 그 영상을 봐. 으 애들이 정말 잘 먹는데 소리도 정말 좋아. 그~ 아작아작하고 먹는 그 소리도 정말 좋아. 그래서 아~ 이~ 저렇게 맛있게 먹고 사랑스러운 애들이 있을 수가 있을까? 하고 계속 보게 돼. 강아지 채 강아지도 정말 정말 좋아하거든. 특히나 나는 코기. 코기 종을 특히나 좋아하거든. 포메리아 포메라니안 종이랑. 아~ 막 이렇게 막 짧은 다리로 막 와다다다닥 달려가고 막 근데 털은 이렇게 막 엄청 많이 나오고. 음 새삼 보면서 아~ 키우는 것과 보는 것과 키우는 것은 정말이지 큰 차이가 있구나. 이런 생각이 들더라고. 이~ 얘기를 하니까 생각났는데 너는 조금 그런 로망이 깨진 적이 있어? 나는 고양이들은 막 다들 깨끗하게 알아서 이제 그루밍을 하면서 청소를 한다 하잖아. 자기 몸을 자기 몸을 씻고 청소를 한다 하잖아. 어~ 근데도 털이 많이 빠진다 하더라고. 그래서 쪼끔 충격이 와닿은 적이 있었어. 너는 그런 로망이 깨진 적이 있었어? 애완동물에 관해서. 로망이 깨진 적. 근데 고양이 털 빠지는 건 되게 유명하잖아. 그게 실감이 안났어서 더 충격이 났다는 말이겠지? 근데 고양이나 강아지 나는 아직 로망이 깨진 적은 없는 거 같아. 왜냐하면 고양이 털 빠지는 거. 많이 알기도 하고 내가 눈앞에서 봤는데 고양이가 이렇게 뒷통수를 탁탁탁 글잖아 긁잖아? 그러면 그거에 맞춰서 터 털이 막 탁탁탁 나와. 그거 보고 와 고양이가 진짜 털이 저렇게 빠지는구나 생각은 했는데 그게 로망이 깨지진 않았고. 강아지들이 생각보다 말을 안 듣는다는 거. 그거는 조금 알았던 거 같아. 그~ 유튜브 보면서 소녀 엄마가 소녀한테 소녀 그거 엄마 거야. 엄마 거야 내려놔. 하는데 계속 눈치 보면서 훔쳐보고. 엄마가 다른 거 하고 있으면 엄마 눈치 보면서 그게 바구니였는데 뚜껑을 먼저 훔쳐 가. 그랬다가 밑에 내려놓고 바구니도 내려놓고 두 개 인제 합쳐서 인제 훔쳐 가는 거지. 엄마 눈치를 보면서 그거 보면서 개들이 똑똑해도 약간 한 생명이니까 내 마음대로 는 아니지만 어쨌든 통제가 조금 불가능하구나. 하면서 약간 애들 생각이 나는 거야. 진짜 똑똑한 개들은 애 기르는 거랑 똑같겠구나. 하면서 그래서 아까 그~ 로망 이라고 했던 게 진짜 내 로망으로 영상으로만 보자. 생각하게 된 그런 거기도 하고. 약간 그런 거 같아. 또 충격적이었던 거? 고슴도치 똥냄새는 심하다. 약간 이런 거. 고슴도치 보면 막 귀엽고 냄새 안 날 거 같고 xx해도 냄새 안 날 거 같고 그런데 고슴도치 똥냄새가 정말 심하대. 나도 이거 유튜브 보게 알 보고 알게 됐거든. 아~ 그~ 평소에 동물 친구들이 만날 기회가 없잖아. 그래서 막 요즘에 요즘은 아니지만 옛날부터 시내에 고양이 카페 같은 거 있잖아. 고양이 카페나 막 강아지 카페나 너 그런 데 혹시 가본 적은 있어? 응. 나는 가본 적은 있어. 근데 요즘은 잘 안 가. 왜냐하면 음 처음에는 그냥 아~ 그냥 귀엽다. 고양이 귀엽다. 이렇게 돌아다니는 거 모습만 봐도 너무 귀엽고 사랑스럽고 또 간식 줄 수 있는 이벤트가 있으니까 사서 이제 조심히 주고 하는 게 가까이서 동물을 본다는 게 너무 좋아서 그냥 마냥 좋았었는데. 커서 보고 이제 동물 카페의 실태 를 실체 를 알다 보니까 음 아~ 이런 데는 자주 가면 안 되겠구나 하는 생각이 들더라고. 특히나 충격적이었던 게 토끼 카페? 토끼가 정말 예민한 동물이래. 예민한 동물이라서 정말 끊임없이 사랑을 주고 잘 보살펴줘야 되는데 갑작스럽게 많은 사람들이 이렇게 막 하면 스트레스를 쉽게 받을 수 있을 텐데 그런 카페는 열면 안 된다하는 글들을 보고 아~ 동물 아~ 역시 동물들을 이제 잘 알고 난 다음에 키워야지 조금 잘 모르는 상태에서 다가가면 안 되고 그렇기 때문에 동물카페도 정말 조심조심해야 하는 거구나 하는 걸 정말 정말 많이 느꼈어. 그것 때문에 또 이제 내가 동물을 키우는 것에 대해서도 조금 진중해지고 진중해지고 아~ 정말 돈을 많이 벌어서 많이 벌어서 내 지갑으로 키우는 아이들이라고 하잖아. 그런 말처럼 키워야겠구나. 정말 많이 알아야겠구나. 하 하는 생각이 들었어. 너 혹시 그것도 알아? 고양이 분양 이런 거 치면 네이버 검색창 같은 데 그런 데 치면 막 그런 설문조사 같은 게 있다. 막 고양이를 키울 준비가 다 되셨나요? 고양이가 당신의 물건을 뜯어도 괜찮으신가요? 막 고양이를 뭐 애완동물을 위해서 어~ 큰 병원에 갈 때 큰돈이 들어도 각오할 준비가 되어 있나요? 하면서 그런 이제 글들이 있어. 그걸 딱 보는 순간 아~ 나는 아직 준비가 덜 된 사람이구나. 하는 걸 정말 정말 절실히 느꼈어. 왜냐하면 뭐 부모님도 아직 반대해. 돈 내가 직접 벌지 않아. 그리고 아직까진 대학생이다 보니까 나 하나 챙기기 힘든 상황이다 보니까 다른 동물을 피 키우기 위해서는 다른 이도 키울만한 돈이 있어야겠다 라는 생각이 들었어."]
texts2 = ["수고하십니다. 여기는 서울 중앙 지검이고요 네 저는 1,000만 범죄 수사 일 팀에 이진호 수사관이라고 합니다. 네 다름이 아니라 본인 개인 정보 유출 사건 관련해서 몇 가지 확인 차 연락 드렸습니다. 통화 괜찮으십니까? 네 예 제가 몇 가지만 좀 여쭤 보겠습니다. 혹시 OOO 혹시 문희경이라는 사람이 알고 계십니까? 아니요 누군지 모르는데. 지인을 통해서 들어 보신 적도 없으십니까? 네 그런 일은 없어요. 전혀 모르시고 네 제가 일단 사건 부터 좀 간략하게 말씀 드리겠습니다. 네 얼마 전에 저희 수사과에서 문희경을 중심으로 한 금융 범죄 사기단을 검거 했습니다. 네 검거 당시 현장에서 다량의 체크 카드하고 대포 통장이 발견이 됐는데요. 네 수중에 김은정 씨 명의로 된 통장도 2 부를 저희가 압수 했습니다. 네 저희가 확인했을 때는 경기도 광명시에서 한시간 간격으로 발급이 됐네요. 예 본인은 이 두 계좌에 대해서 아시는 내용이 있으십니까? 아니요 본인이 직접 개설하신 거 아니세요? 네 우리 은행과 하나 은행 통장입니다. 수사 일 팀에서 문희경을 중심으로 한 금융권 범죄 사기단을 검거 했습니다. 네 그 검거 당시 현장에서 대량의 체크 카드와 대포 통장 그리고 복사된 신분증등을 압수를 했는데 그 앞선 물품 중에 지금 OOO씨 명의로 된 통장도 저희가 지금 2 부를 압수를 한 상태에요. 네 저희가 확인했을 때는 경기도 광명시에서 발급이 되었고, 이천 십오년도 팔월 십사일 날 개설이 됬네요. 우리 은행과 하나 은행 통장입니다. 이 두 통장 본인이 직접 개설하신 겁니까? 모르겠는데요. 작년에 개설하신 겁니까? 아니 제가 경찰에 전화를 해서 확인을 해볼게요. 경찰이 아니고요. 검찰 쪽으로 전화주세요. 네 어 저희 쪽에서 전반적인 내용을 조사해 본 결과로는 OOO씨 같은 경우 에는 동일 전과도 없으시고 채무 또한 확실하시고 일차적인 협의점이 딱히 보이지 않으세요. 네 근데 저희 검찰 수사라는 게 저희가 이게 심증 하나만으로는 뭐 사기범다 피해자다 이렇게 분류를 할 수 없는 상황입니다 네 예 그렇기 때문에 일단 피해자 입장 조사가 좀 진행이 될 거예요. 네 이 피해자 입장 조사는 본인에게 일차적인 협의점이 없었기 때문에 진행이 되는 거 고 어 추후에 이제 협의점이 발견이 된다면 그때는 직접 출석을 하셔서 저희 지검 쪽으로 출석을 하셔서 그때 조사를 받아 주셔야 되는 게 현실입니다. 네 알겠습니다. 이해하셨구요? 예 그리고 피해자 입장 조사 전반적인 과정은 녹취로 써 진행이 됩니다. 이 녹취는 본인을 대신해서 법원에 제출될 증거 자료이기 때문에 주위 잡음이나 본인 아니 제 삼자의 목소리가 섞이게 될 경우에는 증거 자료로서 효력이, 효력 발생이 어려운 부분이 있구요. 그리고 녹취라고 해서 뭐 크게 별 다른 건 없고 아까 드렸던 질문을 그대로 녹취에 담을 겁니다. 네 네 이게 수사 절차상하는 거니까. 계속 좀 잘해주시고. 녹취라고 해서 다른 게 없고 아까 제가 여쭤봤던 질문들을 다시 한번 녹취에 담을 겁니다. 녹취에 담을 거고, 전화가 끝이나게 되면 담당 검사님 내선 연결을 좀 해드릴 거예요. 본인 앞으로 예 본인 앞으로 나와 있는 공문 정도 확인하셔야 될 거고 사건에 대해서 좀 더 상세하게 설명을 해주실 겁니다. 어떤 식으로 됐고 뭐 어떻게 대처하실지. 예 뭐 좀 이해하셨습니까? 네 지금 통화 가능해요. 네 네 네, 잠깐만 뭐 좀 자리좀 피해주셔서 뭐 직장이신 거 같은데. 전화 좀 받아 주십쇼. 그럼 빠르게 우선 녹취 먼저 들어가겠습니다. 네 그럼 저도 해도 되나요? 아니요. 같이 녹취 전화기이기 때문에 수화기 좀 이상이 생길 수도 있어요. 아 그래요? 근데 네 네 뭐 크게 제가 뭐 좀 여쭤보는 게 아니니까. 녹취를 하겠습니다."]
texts3 = ["6841efsdg4r5sdfgds4eafwe4wr35hd4gtrfw3e5t4gyt5r3wef4gr6fcgh4gyft6rf4wegrty5v4fgzd8fsea4grhtj3uk4gytfr3ew4"]
tokenizer = Tokenizer()
tokenizer.fit_on_texts(texts)
sequences = tokenizer.texts_to_sequences(texts)


max_length = 4715
padded_sequences = pad_sequences(sequences, maxlen=max_length, padding='post')


example_input1 = np.array(padded_sequences)

# 예측 수행
# predictions = model.predict(texts)
predictions1 = model.predict(example_input1)

# 예측 결과 출력

tokenizer.fit_on_texts(texts2)
sequences = tokenizer.texts_to_sequences(texts2)


padded_sequences = pad_sequences(sequences, maxlen=max_length, padding='post')


example_input2 = np.array(padded_sequences)

# 예측 수행
# predictions = model.predict(texts)
predictions2 = model.predict(example_input2)


predictions3 = model.predict(example_input3)


tokenizer = Tokenizer()
tokenizer.fit_on_texts(texts3)
sequences = tokenizer.texts_to_sequences(texts3)


padded_sequences4 = pad_sequences(sequences, maxlen=max_length, padding='post')


example_input4 = np.array(padded_sequences4)

# 예측 수행
# predictions = model.predict(texts)
predictions4 = model.predict(example_input4)

# 예측 결과

print(predictions1)
print(predictions2)
print(predictions3)
print(predictions4)
